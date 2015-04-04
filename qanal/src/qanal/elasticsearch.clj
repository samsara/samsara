;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
(ns qanal.elasticsearch
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.bulk :as esb]
            [qanal.messagecodec :as codec]
            [taoensso.timbre :as log]
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


(defn- msgs-reduction-fn
  "This reduction function is applied to sequence of kafka messages and reduces it to a map in the following structure
   {:bulk-operations  bulk-operations
    :stats            stats}

   The bulk-operations is a vector of elasticsearch json bulk commands
   (http://www.elastic.co/guide/en/elasticsearch/reference/1.3/docs-bulk.html) with the bulk commands representing
   bulk operations for each kafka message which has been read/converted. When a invalid kafka message is encountered,
   a warning message is logged and the message is essentially skipped (i.e (into [] nil))

   The stats is a map containing statistics that would involve fully realising a lazy sequence and are better collected
   when the entire sequence is traversed as it is here.
  "
  [{:keys [bulk-operations] :as reduced-map} {:keys [offset] :as msg}]
  (let [bulk-op (msg->bulk-operation msg)
        updated-reduced-map (assoc-in reduced-map [:stats :kafka-msgs-last-offset] offset)
        kafka-msgs-count (get-in updated-reduced-map [:stats :kafka-msgs-count] 0)
        updated-reduced-map (assoc-in updated-reduced-map [:stats :kafka-msgs-count] (inc kafka-msgs-count))
        updated-reduced-map (assoc updated-reduced-map :bulk-operations (into bulk-operations bulk-op))]
    updated-reduced-map))

(defn bulk-index [{:keys [end-point]} kafka-msgs]
  (let [reduced-map (reduce msgs-reduction-fn {:bulk-operations []} kafka-msgs)
        bulk-operations (:bulk-operations reduced-map)
        stats (:stats reduced-map)
        els-conn (esr/connect end-point)
        bulk-resp (esb/bulk els-conn bulk-operations)
        updated-stats (assoc stats :bulked-docs (count (:items bulk-resp)))
        updated-stats (assoc updated-stats :bulked-time (:took bulk-resp))]
    (when (:errors bulk-resp)
      (log/warn "Failed to execute entire bulk index. Response->" bulk-resp))
    updated-stats))

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
                       :consumer-offset 70 :partition 9
                       :key "iamakey" :value test-river-data})

  (def test-kafka-msg1 {:topic "kafkatopic"
                       :consumer-offset 70 :partition 9
                       :key "iamakey" :value (-> test-river-map1 json/encode .getBytes)})

  (def test-kafka-msg2 {:topic "kafkatopic"
                        :consumer-offset 72 :partition 9
                        :key "iamakey" :value (-> test-river-map2 json/encode .getBytes)})

  (bulk-index [test-kafka-msg1 test-kafka-msg2] {:end-point "http://localhost:9200"})
  (reduce msgs-reduction-fn {:bulk-operations []} [test-kafka-msg1 test-kafka-msg2])
  (msg->bulk-operation test-kafka-msg)
  )
