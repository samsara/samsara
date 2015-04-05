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
(ns qanal.kafka
  (:import (kafka.javaapi.consumer SimpleConsumer))
  (:require [qanal.utils :refer [result-or-exception bytes->string from-json safe-short]]
            [clj-kafka.zk :refer (brokers committed-offset set-offset!)]
            [clj-kafka.core :refer (ToClojure)]
            [clj-kafka.consumer.simple :refer (consumer topic-meta-data messages topic-offset)]
            [schema.core :as s]
            [samsara.trackit :refer [track-time]]
            [taoensso.timbre :as log]))


;; TODO this should be part of clj-kafka.core namespace
;; will remove once pull request (https://github.com/pingles/clj-kafka/pull/40) is part of clj-kafka release
(extend-protocol ToClojure
  nil
  (to-clojure [x] nil))

(defn- meta-data->lead-broker-data [m partition-id]
  (let [partition-meta-seq (:partition-metadata m)
        some-fn (fn [pmd] (when (= partition-id (:partition-id pmd)) (:leader pmd)))]
    (some some-fn partition-meta-seq)))

(defn- get-lead-broker [{:keys [host port topic partition-id group-id]}]
  (let [c (consumer host port group-id)
        topic-meta-seq (topic-meta-data c (vector topic))]
    (log/debug "Topic Meta Data from Zookeeper -> " topic-meta-seq)
    (first (keep #(meta-data->lead-broker-data % partition-id) topic-meta-seq))))

(defn connect-to-lead-broker [{:keys [topic partition-id zookeeper-connect group-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}
        kvs [:topic topic :partition-id partition-id :group-id group-id]
        brokers-seq (map #(apply assoc % kvs) (brokers zookeeper-props))
        lead-broker (some get-lead-broker brokers-seq)]
    (log/debug "Zookeeper cluster[" zookeeper-connect "] provided these kafka brokers ["
               brokers-seq "]")
    (if lead-broker
      (do
        (log/info "Lead broker for topic[" topic "] partition[" partition-id "] ->" lead-broker)
        (consumer (:host lead-broker) (:port lead-broker) group-id))
      (log/warn "Lead Broker NOT found for topic[" topic "] partition-id[" partition-id "] !!"))))




(defn from-json-safe
  "Json parsing with exception handling"
  [^String m]
  (safe-short nil (str "Unable to parse this json message |" m "|")
        (from-json m)))


(def validate-river-format
  "Validation schema for incoming messages"
  {(s/required-key :index)  s/Str
   (s/required-key :type)   s/Str
   (s/optional-key :id)     s/Str
   (s/required-key :source) {s/Any s/Any}})


(defn validate-message
  "Validate format of incoming message"
  [msg]
  (when msg
    (safe-short nil (str "Invalid message format: " (prn-str msg))
          (s/validate validate-river-format msg))))


(defn unmarshall-values
  "Decodes json messages into clojures maps, or set it to nil if it can't"
  [msg]
  (update-in msg [:value] (comp validate-message from-json-safe bytes->string)))


(defn get-messages
  "For a given topic/partition and an offset, fetches the next messages
  up to a given size."
  [^SimpleConsumer consumer
   {:keys [group-id topic partition-id consumer-offset fetch-size]}]
  (track-time (str "qanal.kafka.fetch-messages." topic "." partition-id)
     (doall
      (map unmarshall-values               ;; conv bytes to clojure maps
           (messages consumer group-id     ;; fetch messages
                     topic partition-id
                     consumer-offset fetch-size)))))



(defn get-consumer-offset [{:keys [zookeeper-connect group-id topic partition-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (committed-offset zookeeper-props group-id topic partition-id)))


(defn get-topic-offset [^SimpleConsumer consumer {:keys [topic partition-id auto-offset-reset]}]
  {:pre [(#{:earliest :latest} auto-offset-reset)]}
  (topic-offset consumer topic partition-id auto-offset-reset))


(defn get-earliest-topic-offset [^SimpleConsumer consumer m]
  (get-topic-offset consumer (assoc m :auto-offset-reset :earliest)))

(defn get-latest-topic-offset [^SimpleConsumer consumer m]
  (get-topic-offset consumer (assoc m :auto-offset-reset :latest)))

(defn set-consumer-offset [{:keys [zookeeper-connect group-id topic partition-id consumer-offset]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (set-offset! zookeeper-props group-id topic partition-id consumer-offset)))

(defn calculate-partition-backlog [consumer {:keys [consumer-offset] :as m}]
  (let [latest-zk-offset (result-or-exception get-latest-topic-offset consumer m)
        backlog (if (instance? Exception latest-zk-offset) -1 (- latest-zk-offset consumer-offset))]
    (when (instance? Exception latest-zk-offset)
      (log/warn latest-zk-offset "Couldn't calculate backlog due to Exception, returning backlog as -1"))
    backlog))

(comment

  (def con (connect-to-broker {:topic "river" :partition-id 0 :zookeeper-connect "localhost:49155" :group-id "someclient"}))
  (->>
    (get-messages con {:group-id "someclient" :topic "river" :partition-id 0 :consumer-offset 0 :fetch-size (* 10 1024 1024)})
    (map #(-> % :value String. println)))
  )
