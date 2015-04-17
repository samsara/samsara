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
            [taoensso.timbre :as log]
            [samsara.trackit :refer [track-time track-rate]]))


(defn make-bulk-request
  "Turns a kafka message into a two-lines bulk operation item.
  see http://www.elastic.co/guide/en/elasticsearch/reference/1.3/docs-bulk.html
  for more information."
  [{:keys [index type id source]}]
  [ {:index
     (merge {:_index index,
             :_type type }
            (when id {:_id id})) }
    source ])


(defn bulk-index [{:keys [end-point]} messages]
  ;; TODO: connection should be cached
  (let [conn (esr/connect end-point)
        ;; track interesting metrics
        _    (track-rate "qanal.els.bulk-index.docs" (count messages))
        resp (track-time "qanal.els.bulk-index.time"
                              (esb/bulk conn (mapcat make-bulk-request messages)))]
    ;; TODO: better error han
    ;; maybe track num failures
    (when (:errors resp)
      (log/warn "Failed to execute entire bulk index. Response->" resp))))


(comment
  (def test-endpoint (esr/connect "http://localhost:9200"))

  (def test1 {:index "test_index"
              :type "test_type"
              :id "HaShMe"
              :source {:first-name "Kelis"
                       :surname "WaterDancer"}})

  (def test2 {:index "test_index"
              :type "test_type"
              :source {:first-name "Luke"
                       :surname "Skywalker"}})

  (def test3 {:index "test_index"
              :type "test_type"
              :source {:first-name "Obiwan"
                       :surname "Kenobi"}})

  (mapcat make-bulk-request [test1 test2 test3])
  (bulk-index test-endpoint [test1 test2 test3]))
