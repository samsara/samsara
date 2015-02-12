(ns qanal.elsasticsearch
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.bulk :as esb]
            [kasiphones.messagecodec :as codec]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(defn- river-map->bulk-index-operation [river-map]
  (let [{:keys [index type source id]} river-map
        operation-map {:index {:_index index :_type type }}
        operation-map (if (nil? id) operation-map (assoc-in operation-map [:index :_id] id))
        document source]
    [operation-map document]))

(defn- msg->bulk-operation [{:keys [value] :as msg}]
  (let [river-map (codec/decode-river-json value)]
    (if (nil? river-map)
      (log/warn "Unable to decode message and thus will NOT be indexing it. Message details -> " msg)
      (river-map->bulk-index-operation river-map))))


(defn- msgs-reduction-fn [{:keys [bulk-operations] :as reduced-map} {:keys [offset] :as msg}]
  (let [bulk-op (msg->bulk-operation msg)
        updated-m (assoc-in reduced-map [:stats :last-offset] offset)
        updated-m (assoc updated-m :bulk-operations (into bulk-operations bulk-op))]
    updated-m))

(defn bulk-index [msg-seq {:keys [end-point]}]
  (let [reduced-map (reduce msgs-reduction-fn {:bulk-operations []} msg-seq)
        bulk-operations (:bulk-operations reduced-map)
        stats (:stats reduced-map)
        els-conn (esr/connect end-point)
        bulk-resp (esb/bulk els-conn bulk-operations)]
    (if (:errors bulk-resp)
      (log/warn "Failed to execute entire bulk index. Response->" bulk-resp)
      (log/info "Bulk indexed " (count (:items bulk-resp)) " documents in " (:took bulk-resp) " millisecs"))
    stats))

(comment
  (require '[cheshire.core :as json])
  (def test-endpoint (esr/connect "http://localhost:9200"))

  (def test-river-map {:index "test_index"
                       :type "test_type"
                       :id "HaShMe"
                       :source {:first-name "Kelis"
                                :surname "WaterDancer"}})

  (def test-river-map1 {:index "test_index"
                       :type "test_type"
                       :source {:first-name "Luke"
                                :surname "Skywalker"}})

  (def test-river-map2 {:index "test_index"
                        :type "test_type"
                        :source {:first-name "Obiwan"
                                 :surname "Kenobi"}})

  (def test-river-json (json/encode test-river-map))
  (def test-river-data (.getBytes test-river-json))
  [topic offset partition key value]
  (def test-kafka-msg {:topic "kafkatopic"
                       :offset 70 :partition 9
                       :key "iamakey" :value test-river-data})

  (def test-kafka-msg1 {:topic "kafkatopic"
                       :offset 70 :partition 9
                       :key "iamakey" :value (-> test-river-map1 json/encode .getBytes)})

  (def test-kafka-msg2 {:topic "kafkatopic"
                        :offset 72 :partition 9
                        :key "iamakey" :value (-> test-river-map2 json/encode .getBytes)})

  (bulk-index [test-kafka-msg1 test-kafka-msg2] {:end-point "http://localhost:9200"})
  (reduce msgs-reduction-fn {:bulk-operations []} [test-kafka-msg1 test-kafka-msg2])
  (msg->bulk-operation test-kafka-msg)
  )
