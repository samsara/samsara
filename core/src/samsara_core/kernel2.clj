(ns samsara-core.kernel2
  (:refer-clojure :exclude [var-get var-set])
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [moebius
             [core :refer [where inject-if]]
             [kv :as kv]]
            [samsara.utils :refer [from-json to-json]]
            [taoensso.timbre :as log])
  (:require [samsara.trackit :refer [track-time track-rate new-registry
                                     count-tracker distribution-tracker
                                     track-pass-count] :as trk]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                           ---==| T O D O |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; TODO: tracking needs to be reviewed
;; TODO: display of events
;; TODO: per message vs per batch error handling
;; TODO: handling of events which not match any route
;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;     ---==| S A M S A R A   K E R N E L   D I S P A T C H I N G |==----     ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn process
  "Returns a function which takes a collection of values
   and it applies the given handler to the values as a whole"
  [handler]
  (fn [values]
    (handler values)))



(defn- all-pairs?
  "returns true iff coll is formed one or more vectors of 2 elements"
  [coll]
  (and (->> coll (map count) (cons 2) (apply =))
     (> (count coll) 0)))



(defn- add-grouping-keys
  "given a list of pairs [disp-fn handler-fn] it adds a grouping
   key used for internal dispatch and it returns a list of
   [:disp-key disp-fn handler-fn]."
  [specs]
  (map (fn [[tf df]]
         [(keyword (gensym)) tf df])
       specs))



(defn- update-default-route-dispatch-fn
  "given a list of pairs [disp-fn handler-fn] if it finds a :default
   route it adds a dispatch-fn which matches everything"
  [specs]
  (map (fn [[tf df]]
         (let [testf (if (= tf :default) (constantly true) tf)]
           [testf df]))
       specs))



(defn- normalize-dispatch-table
  "update the routing table format to the expected internal fmt."
  [specs]
  (-> specs
      update-default-route-dispatch-fn
      add-grouping-keys))



(defn- match-dispatcher
  "It tries to match the individual element to the given route.
   It test every element with the dispatch-fn and if the test is a
  truthy value then it returns the key of the first matching
  route. Routes are matched in the given order."
  [expanded-specs]
  (fn [element]
    (some
     (fn [[k dsf _]]
       (when (dsf element)
         k))
     expanded-specs)))



(defn dispatch-group-by
  "It takes a list of pairs as [dispatch-fn handler-fn] and it returns
  a function which will take a list of values for which each element
  will be processed according which dispatch-fn returns a truthy
  value. You can add a :default route which will match all not yet
  matched elements. Routes are processing in the given order, and the
  element will be dispatched to the first dispatching rule which
  matches. Each group is dispatched as a single (handler all-matching-values)
  call."
  [dispatcher-specs]
  {:pre [(all-pairs? dispatcher-specs)]}
  (let [specs      (normalize-dispatch-table dispatcher-specs)
        dispatcher (match-dispatcher specs)
        handlers   (into {} (map (juxt first last) specs))]
    (fn [values]
      (let [groups (sort-by first (group-by dispatcher values))]
        (mapcat (fn [[k values]] ((handlers k) values)) groups)))))



(defn filter-wrapper
  "A wrapper which filters the element which are passed to the
   handler. The element which are not matching the filter are
   skipped."
  [pred handler]
  (fn [values]
    (handler (filter pred values))))



(defn partition-filter-wrapper
  "A filter which drops all elements coming from partitions which
   are not in the given partitions list.
   partitions can be something like:
      - [0 1 5] to only pass to the handler the messages coming
        from the partitions 0, 1 and 5.
      - :all to pass all the messages without dropping anything. "
  [partitions handler]
  (let [pred (if (= partitions :all)
               (constantly true) (comp (set partitions) :partition))]
    (filter-wrapper pred handler)))



(defn from-json-format-wrapper
  "a wrapper which convert the :message from a json formatted string
   into a clojure data-structure."
  [handler]
  (fn [values]
    (handler (map #(update % :message from-json) values))))



(defn to-json-format-wrapper
  "a wrapper which convert the :message from a clojure data structure
   into a json formatted string."
  [handler]
  (fn [values]
    (handler (map #(update % :message to-json) values))))



(defn message-only-wrapper
  "a wrapper which passes passes only the :messages to the handler
   without the metadata wrapper."
  [handler]
  (fn [values]
    (handler (map :message values))))



(defn topic-dispatcher
  "a wrapper which takes a list of values an turns each element into

       {:topic \"a-topic\" :key \"a-partition-key\" :mesasge the-element}

  where the topic is decided by (topicf element) and similarly the key
  is (keyf element). The returned key can be nil (which in turn will
  dispatch the message to a random partition), but the topic must
  return a non-nil value."
  [topicf keyf handler]
  (let [dispatcher (fn [x] {:topic (topicf x) :key (keyf x) :message x})]
    (fn [values]
      (handler (map dispatcher values)))))


;; TODO: add :string and :bytes
(defn format-wrapper
  "a wrapper which parses the :message from the appropriate format"
  [format handler]
  (case format
    :json   (from-json-format-wrapper handler)
    handler))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| S T A T E   M A N A G E M E N T |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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

(declare build-routes)

(defrecord SamsaraCore
    [config            ;; normalized configuration currently used
     routes            ;; route dispatchers
     global-stores     ;; global-stores for dimensions
     output-collector]
    component/Lifecycle


  (start [{done :route config :config :as this}]
    (if-not done
      (-> this
          (assoc :route (build-routes this))
          (assoc :global-stores    (atom {}))
          (assoc :output-collector (atom nil)))
      this))


  (stop [{done :route :as this}]
    (if done
      (-> this
          (assoc :route            nil)
          (assoc :global-stores    nil)
          (assoc :output-collector nil))
      this)))



(defn- kv-restore
  "Restores the data from a txlog stream into a give kv-store.
   If the kv-store is nil an empty in-memory is created instead."
  [kvstore txlog-events]
  (kv/restore (or kvstore (kv/make-in-memory-kvstore)) txlog-events))



(defn global-kv-restore
  "Takes a bunch of tx-log events and restores them in the global-stores"
  ([{:keys [component stream-id events]}]
   (global-kv-restore component stream-id events))
  ([{:keys [global-stores config] :as component} stream-id events]
   (swap! global-stores
          update
          stream-id
          kv-restore events)
   []))



(defn local-kv-restore
  "Takes a bunch of tx-log events and restores them in the stream local-stores"
  ([{:keys [component stream-id events]}]
   (let [kv-store (:local-store (stream-config component stream-id))]
     (var-set kv-store
              (kv-restore (var-get kv-store) events)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;          ---==| S Y S T E M   I N I T I A L I Z A T I O N |==----          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(def ^:const DEFAULT-STREAM-CFG
  {:input-partitions          :all
   :state                     :none
   :format                    :json
   :processor                 "samsara-core.core/make-samsara-processor"
   :processor-type            :samsara-factory
   :bootstrap                 false
   :output-topic-partition-fn :sourceId})



(defn- stream-defaults
  "Apply defaults for a stream configuration"
  [{:keys [input-topic state-topic state processor processor-type] :as stream}]
  (as-> stream is

    ;; add default state-topic configuration
    (inject-if is (and (= :partitioned state) (not state-topic))
               :state-topic (str input-topic "-kv"))

    (inject-if is (and (= :partitioned state) (not state-topic))
               :local-store (thread-local (kv/make-in-memory-kvstore)))


    ;; when state is global use global-kv-restore!
    (inject-if is (and (= :global state) (not processor))
               :processor #'global-kv-restore)

    (inject-if is (and (= :global state) (not processor))
               :processor-type :component-stream-processor)


    ;; when state is global use global-kv-restore!
    (inject-if is (and (= :global state) (not processor-type))
               :processor-type :component-stream-processor)

    ;; merge defaults
    (merge DEFAULT-STREAM-CFG is)))



(defn- normalize-streams-with-defaults
  "apply streams defaults"
  [config]
  (-> config
      (assoc  :priority (mapv :id (:streams config)))
      (update :streams
              (fn [streams]
                (->> streams
                     (map stream-defaults)
                     (map (juxt :id identity))
                     (into {}))))))



(defn make-samsara-core [config]
  (let [cfg (normalize-streams-with-defaults config)]
    (map->SamsaraCore {:config cfg})))



(defn stream-config
  "Return the configuration for the given stream"
  [component stream-id]
  (get-in component [:config :streams stream-id]))



(defn component-stream-processor-wrapper
  [component stream-id handler]
  (fn [values]
    (handler {:component component :stream-id stream-id :events values})))


(defn- standard-stream-wrappers
  [component stream-id handler]
  (let [{:keys [format input-partitions]} (stream-config component stream-id)]
    (->> handler
         (message-only-wrapper)
         (format-wrapper format)
         (partition-filter-wrapper input-partitions))))


(defn- tx-log->formatter
  "Return a function which turns the tx-log into a a triplet of

   [topic partition-key json-data]"
  [kvstore-topic]
  (fn [txlog]
    (->> txlog
         (map (fn [[k ver v]]
                [kvstore-topic k (to-json [k ver v])])))))

;; TODO: change txlog format look like events
;; TODO: support dispatch-fn and  format fn
(defn topic-process-wrapper [component stream-id pipeline]
  (let [{:keys [local-store state-topic
                output-topic-partition-fn output-topic ]
         } (stream-config component stream-id)
        output-collector (:output-collector component)
        tx-log->messages (tx-log->formatter state-topic)
        out-dispatch (fn [event] [output-topic
                                 (output-topic-partition-fn event)
                                 (to-json event)])]
    (fn [events]
      (let [state    (var-get local-store)
            [new-state rich-events] (pipeline state events)
            txlog     (kv/tx-log new-state)
            txlog-msg (tx-log->messages txlog)
            output-events (map out-dispatch events)
            all-output (concat txlog-msg output-events)]

        ;; emitting the output
        (@output-collector all-output)

        ;; flushing tx-log
        (let [new-state' (kv/flush-tx-log new-state txlog)]
          (var-set local-store new-state'))))))


(defn load-function-from-name
  ([fqn-fname]
   (if (string? fqn-fname)
     (let [[fns ff] (str/split fqn-fname #"/")]
       (load-function-from-name fns ff))
     fqn-fname))
  ([fn-ns fn-name]
   ;; requiring the namespace
   (require (symbol fn-ns))
   (let [fn-symbol (resolve (symbol fn-ns fn-name))]
     (when-not fn-symbol
       (throw (ex-info (str "function '" fn-ns "/" fn-name "' not found.") {})))
     fn-symbol)))


(defn processor-wrapper
  ([component stream-id]
   (let [{:keys [processor-type processor]} (stream-config component stream-id)]
     (processor-wrapper component stream-id processor-type processor)))

  ([component stream-id processor-type processor]
   (log/debug "Initializing processor [" stream-id "/" processor-type "]:" processor)
   (let [handler (load-function-from-name processor)]
     (case processor-type
       :component-stream-processor
       (component-stream-processor-wrapper component stream-id handler)

       :samsara-factory
       (let [handler' (handler (:config component))]
         (topic-process-wrapper component stream-id handler'))

       :raw
       handler))))



(defn build-routes-for-stream [component stream-id]
  (log/debug "Building routes for:" stream-id)
  (let [{:keys [input-topic state-topic state]} (stream-config component stream-id)]
    (cond

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; build route for partitioned topic with no state
      (= state :none)
      [;; add route to handle topic data
       [(where :topic = input-topic)
        (->> (processor-wrapper component stream-id)
             (standard-stream-wrappers component stream-id))]]


      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; build route for partitioned topic with partitioned state
      (= state :partitioned)
      [;; add route to handle kv-store
       [(where :topic = state-topic)
        (->> (processor-wrapper component stream-id
                                :component-stream-processor local-kv-restore)
             (standard-stream-wrappers component stream-id))]
       ;; add route to handle topic data
       [(where :topic = input-topic)
        (->> (processor-wrapper component stream-id)
             (standard-stream-wrappers component stream-id))]]


      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; build route for global state
      (= state :global)
      [[(where :topic = input-topic)
        (->> (processor-wrapper component stream-id)
             (standard-stream-wrappers component stream-id))]]


      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; build route for CUSTOM managed routes
      (= state :custom)
      [[(where :topic = input-topic)
        (->> (processor-wrapper component stream-id)
             (standard-stream-wrappers component stream-id))]]
      )))


(defn build-routes [component]
  (mapcat (partial build-routes-for-stream component) (:priority (:config component))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                          ---==| T O O L S |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def all-events-counter
  "return a function which is a counter
   when called without argument the counter is increased
   when called with :current-value it returns its current
   value.

      (all-events-counter) ;; increase the counter

      (all-events-counter :current-value)
      ;; => {:count 11}

  "
  (let [tmp-registry (new-registry)
        counter (binding [trk/*registry* tmp-registry]
                  (count-tracker "events.all.counter"))]
    (fn
      ([]
       (counter))
      ([x]
       (case x
         :current-value
         (binding [trk/*registry* tmp-registry]
           (trk/get-metric "events.all.counter"))
         :unknown)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| * S C R A T C H   A R E A * |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (def config (#'samsara-core.main/read-config "./config/config.edn"))
  (def config (read-string (slurp "./config/config.edn")))


  (normalize-streams-with-defaults config)

  (def txlog
    (doall
     (->> (range 30)
          (reduce (fn [kvs x]
               (kv/set kvs
                       (rand-nth ["d1" "d2" "d3"])
                       (rand-nth [:k1 :k2 :k3])
                       x))
             (kv/make-in-memory-kvstore))
          (kv/tx-log)
          (map (fn [m] {:topic "vod-programme"
                       :key   (first m)
                       :partition 0
                       :message (to-json m)})))))

  (def ingest
    (doall
     (->> (repeatedly
           (fn []
             {:timestamp (System/currentTimeMillis)
              :eventName "test"
              :sourceId (rand-nth ["d1" "d2" "d3"])
              :action (rand-int 100)}))
          (take 10)
          (map (fn [x]
                 {:topic "ingestion"
                  :key   (:sourceId x)
                  :partition 0
                  :message x}))
          (map #(update % :message to-json)))))

  (def events (concat txlog ingest))

  (def cmp (component/start (make-samsara-core config)))
  (reset! (:output-collector cmp) (fn [e] (doseq [x e] (prn "OUTPUT:" x)) ))

  (build-routes cmp)

  (build-routes-for-stream cmp :ingestion)

  (-> cmp :global-stores)

  (time ((dispatch-group-by
          (build-routes cmp)) events))

  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn rand-topic []
  (rand-nth ["ingestion" "ingestion-kv" "vod-programme" "brands" "matrix"]))

(defn rand-partition []
  (rand-int 5))

(defn rand-key []
  (str "key-" (rand-int 100)))

(defn rand-msg []
  (to-json
   {:a (rand-nth ["hi" "hola" "ciao" "sia" "czesc" "salut"])
    :b (rand-int 100)
    :c (rand-int 100)}))

(defn rand-element []
  {:topic     (rand-topic)
   :partition (rand-partition)
   :key       (rand-key)
   :message   (rand-msg)})

(defn rand-stream []
  (repeatedly rand-element))

(defonce values (take 30 (rand-stream)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def global (atom []))

(def dispatchers
  [[(where [:or [:topic = "ingestion"] [:topic = "ingestion-kv"]])
    (->> (dispatch-group-by
          [[(where :topic = "ingestion-kv") (message-only-wrapper (fn [v] [{:kv v}]))]
           [:default (message-only-wrapper
                      (topic-dispatcher
                       #(if (even? (:b %)) "even" "odd") :a
                       (to-json-format-wrapper
                        (fn [v] [{:stream v}]))))]])
         from-json-format-wrapper
         (partition-filter-wrapper [1 2]))
    ]

   [(where :topic = "vod-programme")
    (fn [values]
      (->> values
           (map (comp from-json :message))
           (swap! global into))
      [])]

   [(where :topic = "brands")
    (fn [values]
      (->> values
           (map (comp from-json :message))
           (swap! global into))
      [])]

   [:default (fn [v])]])

(comment
  (->> values
       ((process (dispatch-group-by dispatchers))))


  )
