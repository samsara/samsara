(ns samsara-core.kernel
  (:refer-clojure :exclude [var-get var-set])
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]
            [where.core :refer [where]])
  (:require [samsara-core.core :as core])
  (:require [moebius.kv :as kv])
  (:require [samsara.utils :refer [to-json from-json invariant]])
  (:require [samsara.trackit :refer [track-time track-rate new-registry
                                     count-tracker distribution-tracker
                                     track-pass-count] :as trk]))

;;
;;   THIS NEED TO BE COMPLETELY RE-DESIGNED!!!!
;;
;; Things to keep in mind:
;; * stream impedence format
;;   [topic, part, key, msg] -> [state, [event]] ->
;;   -> [topic, key, msg]
;; * batching of messages not a single message
;; * a processable stream will contain data
;;   from multiple topics (ingestion, ingestion-kv)
;;   which need to be processed in a single thread,
;;   here there is a conflict with the idea of batching
;;   which assume items of the same type.
;; * should be able to support raw/untouched streams
;;   which can be in different formats
;; * support for global kv-state
;; * support for stream with no output
;; * support for orthogonal concerns such as
;;   logging, and metrics collecting
;; * handling of exception on a message level
;; * handling of the output-collector done properly
;;

;; runtime pipeline initialized by init-pipeline!
(def ^:dynamic *config*        nil)
(def ^:dynamic *dispatchers*   nil)
(def ^:dynamic *global-stores* nil) ;; thread local only
(def ^:dynamic *stores*        nil)

(def ^:dynamic *print-streams*  false)

(defmacro printf-stream [& args]
  `(when *print-streams*
     (printf ~@args)))

(def tmp-registry (new-registry))
(def all-events-counter (binding [trk/*registry* tmp-registry]
                          (count-tracker "events.all.counter")))

(defn events-counter-stats []
  (binding [trk/*registry* tmp-registry]
    (trk/get-metric "events.all.counter")))


(def ^:const DEFAULT-STREAM-CFG
  {:input-partitions          :all
   :state                     :none
   :format                    :json
   :processor                 "samsara-core.core/make-samsara-processor"
   :processor-type            :samsara-factory
   :bootstrap                 false
   :output-topic-partition-fn :sourceId})


(defn- config-state-topic
  [{:keys [input-topic state-topic state] :as cfg}]
  (if (and (= :partitioned state) (not state-topic))
    (assoc cfg :state-topic (str input-topic "-kv"))
    cfg))


(defn- config-stream
  [stream-cfg]
  (->> stream-cfg
    (merge DEFAULT-STREAM-CFG)
    config-state-topic))


(defn normalize-stream-config [config]
  (update config :streams (partial mapv config-stream)))

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
(defn- make-trackers [stream]
  (let [topic (-> stream :input-topic)
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


(defmulti create-core-processor
  (fn [{:keys [processor-type] :as stream} config] processor-type))


(defmethod create-core-processor :samsara-factory
  [{:keys [id processor] :as stream} config]
  (let [factory (or processor
                   "samsara-core.core/make-samsara-processor")
        [fns ff] (str/split factory #"/")]
    (log/info "Loading processor[" id "]:" factory)
    ;; requiring the namespace
    (require (symbol fns))
    (let [proc-factory (resolve (symbol fns ff))]
      (if proc-factory
        ;; creating the moebius function
        (proc-factory config)
        (throw (ex-info (str "factory '" factory "' not found.") config))))))




(defn make-raw-pipeline
  [{:keys [output-topic-partition-fn output-topic] :as stream} config]
  (let [{:keys [track-time-name
                track-pipeline-time
                size-in-tracker
                size-out-tracker]} (make-trackers stream)
        pipeline (track-pipeline-time (create-core-processor stream config))
        output-topic-fn (constantly output-topic)]
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
                                (map (juxt output-topic-fn output-topic-partition-fn to-json))
                                (map size-out-tracker))])))))))



(defn kv-restore!
  "Takes an event in json format which contain an Tx-Log entry and
   it pushes the change to the running *kv-store*"
  [stream ^String event]
  ;; this ugly stuff works because *stores* is thread-local
  (printf-stream "STATE[%s] : %s\n" stream event)
  (track-time (str "pipeline.stores.local." stream ".restore.time")
    (let [kv-store (get *stores* stream)]
      (var-set kv-store
               (->> event
                    from-json
                    vector
                    (kv/restore (var-get kv-store)))))))



(defn global-kv-restore!
  "Takes an event in json format which contain an Tx-Log entry and
   it pushes the change to the running *global-stores*"
  [stream-id ^String event]
  (printf-stream "GLOBAL[%s] : %s\n" stream-id event)
  (track-time (str "pipeline.stores.global." (name stream-id) ".restore.time")
   (let [txlog (->> event from-json vector)]
     (swap! *global-stores*
            update
            stream-id
            (fn [kvstore]
              (kv/restore (or kvstore (kv/make-in-memory-kvstore))
                          txlog))))))


(defn global-store [store-id]
  (get @*global-stores* store-id))


(defn process-dispatch
  "Takes an event as a JSON encoded String and depending on the stream
   name dispatches the function to the appropriate handler.
   One handler is the normal pipeline processing, the second handler
   is for the kv-store"
  [output-collector stream partition key message]

  (all-events-counter)
  (try
    (if-let [dispatcher (get *dispatchers* stream)]
      (let [{:keys [topic-filter handler]} dispatcher]
        (when (topic-filter partition)
          (handler output-collector stream partition key message)))
      ;; TODO: trow error
      (log/warn "No dispatcher found for:" stream))

    ;;
    ;; ERROR Handling
    ;;
    (catch Exception x
      (log/warn x "Error processing message from [" stream "]:" message)
      (track-rate (str "pipeline." stream ".errors"))
      (output-collector [[(str stream "-errors") key message]]))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn- compile-partition-filter
  [{:keys [input-partitions] :as cfg}]
  (if (= :all input-partitions)
    (constantly true)
    (set input-partitions)))


(defn- tx-log->formatter
  "Return a function which turns the tx-log into a a triplet of

   [topic partition-key json-data]"
  [kvstore-topic]
  (fn [txlog]
    (->> txlog
         (map (fn [[k ver v]]
                [kvstore-topic k (to-json [k ver v])])))))





(defn- compile-input-topic-handler [{:keys [state-topic] :as stream-cfg} config]
  (let [tx-log->messages (tx-log->formatter state-topic)
        pipeline (make-raw-pipeline stream-cfg config)]
    (fn [output-collector stream partition key message]

      (printf-stream "INPUT[%s/%s/%s] : %s\n" stream partition key message)

      ;; other messages are processed by the normal pipeline, however here
      ;; we need to emit the state messages as well.

      ;; this ugly stuff works because *stores* is thread-local
      (let [kv-state (get *stores* stream)
            state    (var-get kv-state)
            [new-state rich-events] (pipeline state message)
            txlog     (kv/tx-log new-state)
            txlog-msg (tx-log->messages txlog)
            all-output (concat txlog-msg rich-events)]

        ;; emitting the output
        (output-collector all-output)

        ;; flushing tx-log
        (let [new-state' (kv/flush-tx-log new-state txlog)]
          (var-set kv-state new-state'))))))


(defn- compile-input-topic-config [{:keys [input-topic state] :as stream} config]
  (when (or (= state :none) (= state :partitioned))
    {input-topic {:topic-filter (compile-partition-filter stream)
                  :handler (compile-input-topic-handler stream config)}}))

(defn- compile-kv-state-handler [{:keys [input-topic state] :as stream}]
  (fn [output-collector stream partition key message]
    (kv-restore! input-topic message)))

(defn- compile-global-kv-state-handler [{:keys [id state] :as stream}]
  (fn [output-collector stream partition key message]
    (global-kv-restore! id message)))

(defn- compile-state-topic-config [{:keys [state-topic state input-topic] :as stream}]
  (cond

    (= state :partitioned)
    {state-topic {:topic-filter (compile-partition-filter stream)
                  :handler (compile-kv-state-handler stream)}}

    (= state :global)
    {input-topic {:topic-filter (compile-partition-filter stream)
                  :handler (compile-global-kv-state-handler stream)}}
    ))


(defn- compile-stream-config
  [stream config]
  (merge
   (compile-input-topic-config stream config)
   (compile-state-topic-config stream)))


(defn compile-config [config]
  (->> config
       :streams
       (mapcat #(compile-stream-config % config))
       (into {})))


(defn init-stores [config]
  (let [streams (map :input-topic
                     (filter (where [:state = :partitioned])
                             (:streams config)))
        _ (log/info "Initializing kv-store for the following streams: " streams)
        local-store (constantly (thread-local (kv/make-in-memory-kvstore)))]
    (into {} (map (juxt identity local-store) streams))))


(defn init-pipeline! [config]
  (log/info "Initialize processors...")
  (alter-var-root #'*config*        (constantly config))
  (alter-var-root #'*stores*        (constantly (init-stores config)))
  (alter-var-root #'*global-stores* (constantly (atom {})))
  (alter-var-root #'*dispatchers*   (constantly (compile-config config))))




(comment
  (def config (#'samsara-core.main/read-config "./config/config.edn"))

  (init-pipeline! config)

  (keys (compile-config config))

  (def stream (-> config :streams first))

  (defn out-> [all-output]
    (println (with-out-str
               (clojure.pprint/pprint all-output))))


  (def msg "{\"publishedAt\":1444755781999,\"receivedAt\":1444755781764,\"timestamp\":1444755781000,\"sourceId\":\"3aw4sedrtcyvgbuhjkn\",\"eventName\":\"user.item.removed\",\"page\":\"orders\",\"item\":\"sku-5433\",\"action\":\"remove\"}")

  (process-dispatch out-> "ingestion" 0 "ciao" msg)

  ((:handler (*dispatchers* "ingestion")) out-> "ingestion" 0 "ciao" msg)

  ((compile-input-topic-handler stream config) out-> "ingestion" 0 "ciao" msg)

  ((tx-log->formatter "ingestion-kv") (kv/tx-log (-> (kv/make-in-memory-kvstore) (kv/set "ciao" :a 1))))

  (def state (kv/make-in-memory-kvstore))
  ((make-raw-pipeline stream config)  state msg)
  ((create-core-processor stream config) state [(from-json msg)])

  (:output-topic-partition-fn stream)

  *global-stores*

  ((:handler (*dispatchers* "dimension1")) out-> "dimension1" 0 "ciao" "[\"ciao\",1,{\"a\":1}]")

  (global-kv-restore! "dimension1" "[\"ciao\",1,{\"a\":1}]")


)
