(ns samsara-core.samza
  (:require [clojure.string :as s])
  (:require [samsara-core.core :as core])
  (:require [moebius.kv :as kv])
  (:require [samsara.utils :refer [to-json from-json invariant]])
  (:require [samsara.trackit :refer [track-time count-tracker distribution-tracker]])
  (:import [org.apache.samza.job JobRunner]
           [org.apache.samza.config MapConfig]))

;; runtime pipeline initialized by init-pipeline!
(def ^:dynamic *pipeline*     nil)
(def ^:dynamic *raw-pipeline* nil)
(def ^:dynamic *config*       nil)
(def ^:dynamic *store*        nil)



(defn samza-config [{:keys [job-name zookeepers brokers offset
                            input-topic kvstore-topic output-topic]}]
  (let [default-kvstore (str input-topic "-kv")
        inputs (->> [input-topic (or kvstore-topic default-kvstore)]
                    (map (partial str "kafka."))
                    (s/join ","))]
    {"job.factory.class" "org.apache.samza.job.local.ThreadJobFactory"
     "job.name"          job-name
     "task.class"        "samsara.SamsaraSystem"
     "task.inputs"       inputs
     "serializers.registry.string.class" "org.apache.samza.serializers.StringSerdeFactory"
     "systems.kafka.samza.factory" "org.apache.samza.system.kafka.KafkaSystemFactory"
     "systems.kafka.samza.key.serde" "string"
     "systems.kafka.samza.msg.serde" "string"
     "systems.kafka.consumer.zookeeper.connect" zookeepers
     "systems.kafka.consumer.auto.offset.reset" (name offset)
     "systems.kafka.producer.bootstrap.servers" brokers

     ;; bootstrapping kv-store
     (str "systems.kafka.streams." input-topic "-kv.samza.bootstrap")    "true"
     (str "systems.kafka.streams." input-topic "-kv.samza.reset.offset") "true"
     (str "systems.kafka.streams." input-topic "-kv.samza.offset.default") "oldest"
     }))



(defn display-samza-config [topics-cfg]
  (println "\t------------------------------ Samza Config ------------------------------")
  (doseq [[k v] (samza-config topics-cfg)]
    (println "\t" k " = " v ))
  (println "\t--------------------------------------------------------------------------"))




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

        track-pipeline-time (fn [pipeline] (fn [x] (track-time track-time-name2 (pipeline x))))

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
    (fn [event]
      (track-time track-time-name
                  (->> event
                       size-in-tracker
                       from-json
                       vector
                       pipeline
                       (map (juxt output-topic part-key-fn to-json))
                       (map size-out-tracker))))))



(defn pipeline
  "Takes an event in json format and returns a list of tuples
  with the partition key and the event in json format
  f(x) -> List<[part-key, json-event]>"
  [^String event]
  (*raw-pipeline* event))



(defn kv-restore!
  "Takes an event in json format which contain an Tx-Log entry and
   it pushes the change to the running *kv-store*"
  [^String event]
  (swap! *store*
         (fn [store]
           (->> event
                from-json
                vector
                (kv/restore store)))))



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



(defn- tx-log->messages [txlog]
  (let [kvstore-topic (kvstore-topic!)]
    (->> txlog
         (map (fn [[k ver v]]
                [kvstore-topic k (to-json v)])))))



(defn uber-pipeline [events]
  (let [rich-events (pipeline events)
        ;;               this not really true when
        ;;               multiple threads running in same vm
        txlog     (kv/tx-log @*store*)
        txlog-msg (tx-log->messages txlog)]
    ;; flushing tx-log
    (swap! *store* kv/flush-tx-log txlog)
    (concat txlog-msg rich-events)))



(defn dispatch
  "Takes an event as a JSON encoded String and depending on the stream
   name dispatches the function to the appropriate handler.
   One handler is the normal pipeline processing, the second handler
   is for the kv-store"
  [^String stream ^String partition ^String event]
  (if (= stream (kvstore-topic!))
    ;; messages for the kvstore are dispatched directly to the restore function
    ;; and this function doesn't emit anything
    (do (kv-restore! event) '())
    ;; other messages are processed by the normal pipeline, however here
    ;; we need to emit the state messages as well.
    (uber-pipeline event)))



(defn init-pipeline! [config]
  (alter-var-root #'*pipeline*     (constantly (core/make-samsara-processor config)))
  (alter-var-root #'*raw-pipeline* (constantly (make-raw-pipeline config)))
  (alter-var-root #'*config*       (constantly config))
  (alter-var-root #'*store*        (constantly (atom (kv/make-in-memory-kvstore))))
  (display-samza-config (:topics config)))


(defn start! [{:keys [topics] :as config}]
  (.run (JobRunner. (MapConfig. (samza-config topics)))))
