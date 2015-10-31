(ns samsara-core.kernel2
  (:require [moebius.core :refer [where]]
            [samsara.utils :refer [from-json to-json]]))

;;
;; Kernel redesign
;;


(defn process [handler]
  (fn [values]
    (handler values)))


(defn- all-pairs? [coll]
  (->> coll (map count) (cons 2) (apply =)))

(defn- add-keys-and-default-route
  [specs]
  (map (fn [[tf df]]
         (let [testf (if (= tf :default) (constantly true) tf)]
           [(keyword (gensym)) testf df]))
       specs))


(defn- match-dispatcher
  [expanded-specs]
  (fn [element]
    (some
     (fn [[k dsf _]]
       (when (dsf element)
         k))
     expanded-specs)))


(defn dispatch-group-by
  [dispatcher-specs]
  {:pre [(all-pairs? dispatcher-specs)]}
  (let [specs      (add-keys-and-default-route dispatcher-specs)
        dispatcher (match-dispatcher specs)
        handlers   (into {} (map (juxt first last) specs))]
    (fn [values]
      (let [groups (sort-by first (group-by dispatcher values))]
        (mapcat (fn [[k values]] ((handlers k) values)) groups)))))


(defn filter-wrapper [pred handler]
  (fn [values]
    (handler (filter pred values))))


(defn partition-filter-wrapper [partitions handler]
  (let [pred (if (= partitions :all)
               (constantly true) (comp (set partitions) :partition))]
    (filter-wrapper pred handler)))


(defn from-json-format-wrapper [handler]
  (fn [values]
    (handler (map #(update % :message from-json) values))))

(defn to-json-format-wrapper [handler]
  (fn [values]
    (handler (map #(update % :message to-json) values))))

(defn message-only-wrapper [handler]
  (fn [values]
    (handler (map :message values))))


(defn topic-dispatcher [topicf keyf handler]
  (let [dispatcher (fn [x] {:topic (topicf x) :key (keyf x) :message x})]
    (fn [values]
      (handler (map dispatcher values)))))

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
       ((process (dispatch-group-by dispatchers)))))
