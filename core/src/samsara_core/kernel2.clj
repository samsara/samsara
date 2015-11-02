(ns samsara-core.kernel2
  (:require [com.stuartsierra.component :as component]
            [moebius
             [core :refer [where]]
             [kv :as kv]]
            [samsara.utils :refer [from-json to-json]]))

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
   into ajson formatted string."
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



(defrecord SamsaraCore
    [config            ;; normalized configuration currently used
     dispatchers       ;; route dispatchers
     global-stores     ;; global-stores for dimensions
     stores]           ;; stream-local data stores (thread-locals)
    component/Lifecycle


  (start [{done :initialized :as this}]
    (if-not done
      (-> this
          (assoc :global-stores (atom {}))
          (assoc :stores        (thread-local (kv/make-in-memory-kvstore)))
          (assoc :initialized   true))
      this))


  (stop [{done :initialized :as this}]
    (if done
      (-> this
          (assoc :global-stores nil)
          (assoc :stores        nil)
          (assoc :initialized   false))
      this)))



(defn global-kv-restore
  "Takes a bunch of tx-log events and restores them in the global-stores"
  [{:keys [global-stores config] :as component} stream-id events]
  ;; TODO: fix tracking
  #_(printf-stream "GLOBAL[%s] : %s\n" stream-id event)
  #_(track-time (str "pipeline."
                     (-> *config* :job :job-name)
                     ".stores.global." (name stream-id)
                     ".restore.time"))
  (swap! global-stores
         update
         stream-id
         (fn [kvstore]
           (kv/restore (or kvstore (kv/make-in-memory-kvstore))
                       events))))



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
  [{:keys [input-topic state-topic state processor] :as stream}]
  (as-> stream it
    ;; add default state-topic
    (if (and (= :partitioned state) (not state-topic))
      (assoc it :state-topic (str input-topic "-kv"))
      it)
    ;; ;; when state is global use global-kv-restore!
    ;; (if (and (= :global state) (not processor))
    ;;   (assoc it :processor-fn #'global-kv-restore)
    ;;   it)
    ;; ;; when state is global use global-kv-restore!
    ;; (if (and (= :global state) (not processor))
    ;;   (assoc it :processor-fn #'global-kv-restore)
    ;;   it)

    ;; merge defaults
    (merge DEFAULT-STREAM-CFG it)))



(defn- normalize-streams-with-defaults
  "apply streams defaults"
  [config]
  (update config :streams
          (partial mapv stream-defaults)))



(defn make-samsara-core [config]
  (let [cfg (normalize-streams-with-defaults config)]
    (map->SamsaraCore {:config cfg})))



(comment
  (def config (#'samsara-core.main/read-config "./config/config.edn"))
  (def config (read-string (slurp "./config/config.edn")))
  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| * S C R A T C H   A R E A * |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



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
