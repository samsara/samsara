(ns samsara-core.samza
  (:refer-clojure :exclude [var-get var-set])
  (:require [clojure.string :as s])
  (:require [samsara-core.core :as core])
  (:require [moebius.kv :as kv])
  (:require [samsara.utils :refer [to-json from-json invariant]])
  (:require [samsara.trackit :refer [track-time count-tracker distribution-tracker]])
  (:import [org.apache.samza.job JobRunner]
           [org.apache.samza.config MapConfig]
           [org.apache.samza.system OutgoingMessageEnvelope
            IncomingMessageEnvelope SystemStream]
           [org.apache.samza.task MessageCollector TaskCoordinator]))

;; runtime pipeline initialized by init-pipeline!
(def ^:dynamic *pipeline*     nil)
(def ^:dynamic *raw-pipeline* nil)
(def ^:dynamic *config*       nil)
(def           *store*        nil) ;; thread local only



(defn samza-config [{:keys [job-name zookeepers brokers offset
                            input-topic kvstore-topic output-topic
                            samza-overrides]}]
  (let [default-kvstore (str input-topic "-kv")
        inputs (->> [input-topic (or kvstore-topic default-kvstore)]
                    (map (partial str "kafka."))
                    (s/join ","))

        config-overrides (into {} (map (fn [[k v]] [(str (name k)) (str v)]) samza-overrides))

        config
        {"job.factory.class" "org.apache.samza.job.local.ThreadJobFactory"
         "job.name"          job-name
         "task.class"        "samsara.SamsaraSystem"
         "task.inputs"       inputs
         "task.checkpoint.factory" "org.apache.samza.checkpoint.kafka.KafkaCheckpointManagerFactory"
         "task.checkpoint.system" "kafka"
         "task.commit.ms"         "60000"

         "serializers.registry.string.class" "org.apache.samza.serializers.StringSerdeFactory"
         "systems.kafka.samza.factory" "org.apache.samza.system.kafka.KafkaSystemFactory"
         "systems.kafka.samza.key.serde" "string"
         "systems.kafka.samza.msg.serde" "string"
         "systems.kafka.consumer.zookeeper.connect" zookeepers
         "systems.kafka.consumer.auto.offset.reset" (name offset)
         "systems.kafka.producer.bootstrap.servers" brokers
         (str "systems.kafka.streams." input-topic ".samza.offset.default") "oldest"

         ;; bootstrapping kv-store
         (str "systems.kafka.streams." input-topic "-kv.samza.bootstrap")    "true"
         (str "systems.kafka.streams." input-topic "-kv.samza.reset.offset") "true"
         (str "systems.kafka.streams." input-topic "-kv.samza.offset.default") "oldest"
         }]
    (merge config config-overrides)))



(defn display-samza-config [topics-cfg]
  (println "\t------------------------------ Samza Config ------------------------------")
  (doseq [[k v] (sort-by first (samza-config topics-cfg))]
    (println "\t" k " = " v ))
  (println "\t--------------------------------------------------------------------------"))


;;
;; Utility classes to handle per thread local state
;;



(defn thread-local
  "Create a thread-local var"
  [init]
  (proxy [ThreadLocal] []
    (initialValue [] init)))



(defprotocol ILocalVar
  (var-set [this new-val] "set the new value of a local var")
  (var-get [this] "return the value of local var"))



(extend-type ThreadLocal

  ILocalVar
  (var-set [this new-val] (.set this new-val))
  (var-get [this] (.get this)))



;; track
;; how many messages in / out
;; how many msg/sec
;; how big is msg in / out
;; distrib of msg size in / out
;; processing time
(defn- make-trackers [config]
  (let [topic (-> config :topics :input-topic)
        prefix (str "pipeline." topic)

        track-time-name (str prefix ".overall-processing.time")
        track-time-name2 (str prefix ".pipeline-processing.time")

        track-pipeline-time (fn [pipeline]
                              (fn [state event]
                                (track-time track-time-name2
                                            (pipeline state event))))

        total-size-in (count-tracker (str prefix ".in.total-size"))
        dist-size-in (distribution-tracker (str prefix ".in.size"))

        total-size-out(count-tracker (str prefix ".out.total-size"))
        dist-size-out (distribution-tracker (str prefix ".out.size"))

        size-in-tracker (invariant #(let [sz (count %)]
                                   (total-size-in sz)
                                   (dist-size-in sz)))
        size-out-tracker (invariant #(let [sz (count (second %))]
                                   (total-size-out sz)
                                   (dist-size-out sz)))

        ]
    {:track-time-name track-time-name
     :size-in-tracker size-in-tracker
     :size-out-tracker size-out-tracker
     :track-pipeline-time track-pipeline-time}))



(defn make-raw-pipeline [config]
  (let [part-key-fn (-> config :topics :output-topic-partition-fn)
        output-topic (constantly (-> config :topics :output-topic))
        {:keys [track-time-name track-pipeline-time size-in-tracker
                size-out-tracker]} (make-trackers config)
        pipeline (track-pipeline-time *pipeline*)]
    (fn [state event]
      (track-time track-time-name
                  (->> event
                       size-in-tracker
                       from-json
                       vector
                       (pipeline state) ;; -> [state [events]]
                       ((fn [[s events]]
                           [s
                            (->> events
                                (map (juxt output-topic part-key-fn to-json))
                                (map size-out-tracker))])))))))



(defn pipeline
  "Takes an event in json format and returns a list of tuples
  with the partition key and the event in json format
  f(state,event) -> [new-state, List<[part-key, json-event]>]"
  [state event]
  (*raw-pipeline* state event))



(defn kv-restore!
  "Takes an event in json format which contain an Tx-Log entry and
   it pushes the change to the running *kv-store*"
  [^String event]
  ;; this ugly stuff works because *store* is thread-local
  (var-set *store*
         (->> event
              from-json
              vector
              (kv/restore (var-get *store*)))))



(defn output-topic!
  "Return the name of the topic used for the output"
  []
  (-> *config* :topics :output-topic))



(defn kvstore-topic!
  "Return the name of the topic used for the K/V store"
  []
  (or
   (-> *config* :topics :kvstore-topic)
   (str (-> *config* :topics :input-topic) "-kv")))



(defn- tx-log->messages
  "Turns the tx-log into a a triplet of [topic partition-key json-data]"
  [txlog]
  (let [kvstore-topic (kvstore-topic!)]
    (->> txlog
         (map (fn [[k ver v]]
                [kvstore-topic k (to-json [k ver v])])))))



(def topic->stream
  "Given a topic it return the equivalent Samza' SystemStream"
  (memoize (fn [topic] (SystemStream. "kafka" topic))))



(defn samza-process
  "Takes an event as a JSON encoded String and depending on the stream
   name dispatches the function to the appropriate handler.
   One handler is the normal pipeline processing, the second handler
   is for the kv-store"
  [^IncomingMessageEnvelope envelope,
   ^MessageCollector collector,
   ^TaskCoordinator coordinator
   ^String stream
   ^String partition
   ^String message]

  (if (= stream (kvstore-topic!))
    ;; messages for the kvstore are dispatched directly to the restore function
    ;; and this function doesn't emit anything
    (kv-restore! message)
    ;; other messages are processed by the normal pipeline, however here
    ;; we need to emit the state messages as well.

    ;; this ugly stuff works because *store* is thread-local
    (let [state (var-get *store*)
          [new-state rich-events] (pipeline state message)
          txlog     (kv/tx-log new-state)
          txlog-msg (tx-log->messages txlog)
          all-output (concat txlog-msg rich-events)]

      ;; emitting the output
      (doseq [[oStream oKey oMessage] all-output]
        ;; TODO: remove this and remove the INPUT: one as well
        (println "OUTPUT[" oStream "/" oKey "]:" oMessage)
        (.send collector (OutgoingMessageEnvelope.
                          (topic->stream oStream) oKey oMessage )))

      ;; flushing tx-log
      (let [new-state' (kv/flush-tx-log new-state txlog)]
        (var-set *store* new-state')))))



(defn init-pipeline! [config]
  (alter-var-root #'*pipeline*     (constantly (core/make-samsara-processor config)))
  (alter-var-root #'*raw-pipeline* (constantly (make-raw-pipeline config)))
  (alter-var-root #'*config*       (constantly config))
  (alter-var-root #'*store*        (constantly (thread-local (kv/make-in-memory-kvstore))))
  (display-samza-config (:topics config)))



(defn start! [{:keys [topics] :as config}]
  (.run (JobRunner. (MapConfig. (samza-config topics)))))
