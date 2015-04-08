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
  (:require [qanal.utils :refer [result-or-exception]]
            [clj-kafka.zk :refer (brokers committed-offset set-offset!)]
            [clj-kafka.core :refer (ToClojure)]
            [clj-kafka.consumer.simple :refer (consumer topic-meta-data messages topic-offset)]
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

(defn get-lead-broker
  "Connects to the broker, at the given host & port, and queries for all known topic meta data.
   If a partition-id that matches the given partition-id is found, then it's lead broker is
   returned in the form of a map (see below) otherwise nil is returned.

   An example of a broker map
   {:connect \"192.1.2.3:9092\"
    :host \"192.1.2.3\"
    :port 9092
    :broker-id 2}
  "
  [{:keys [host port topic partition-id group-id]}]
  (let [c (consumer host port group-id)
        topic-meta-seq (topic-meta-data c (vector topic))]
    (log/debug "Topic Meta Data from broker[" host ":" port "] ->" topic-meta-seq)
    (first (keep #(meta-data->lead-broker-data % partition-id) topic-meta-seq))))

(defn connect-to-lead-broker
  "Returns a kafka.javaapi.consumer.SimpleConsumer that is connected to the lead broker for
   the given partition-id. If no lead broker is found, will return nil"
  [{:keys [topic partition-id zookeeper-connect group-id]}]
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


(defn get-messages
  "Uses the provided arguments to consume kafka messages and return the messages in a lazy sequence.
   The lazy sequence is contains messages of the structure
   (defrecord KafkaMessage [topic offset partition key value])
   topic --> Kafka Topic
   offset --> Offset in the partition
   partition --> partitionid
   key --> Key used to choose partition this message was sent to
   value --> java.nio.ByteBuffer representing the actual content of the message"
  [^SimpleConsumer consumer {:keys [group-id topic partition-id consumer-offset fetch-size]}]
  (messages consumer group-id topic partition-id consumer-offset fetch-size))

(defn get-consumer-offset
  "Returns the stored consumer offset in zookeeper for the provided group-id.
   This offset is stored in the zookeeper location
           /consumers/<group-id>/offsets/<topic>/<partition-id>
   If no offset is found (i.e location is not found) then nil is returned "
  [{:keys [zookeeper-connect group-id topic partition-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (committed-offset zookeeper-props group-id topic partition-id)))

(defn get-topic-offset
  "Queries the broker (lead broker) that the consumer is associated with, and gets the offset specified by
   auto-offset-reset.
   NOTE - only :earliest or :latest are valid values for auto-offset-reset.
   :earliest ---> the earliest message offset that is in the partition
   :latest ---> the latest message offset that is in the partition"
  [^SimpleConsumer consumer {:keys [topic partition-id auto-offset-reset]}]
  {:pre [(#{:earliest :latest} auto-offset-reset)]}
  (topic-offset consumer topic partition-id auto-offset-reset))

(defn get-earliest-topic-offset
  "Retuns the earliest message offset in a partition. See get-topic-offset function for more info"
  [^SimpleConsumer consumer m]
  (get-topic-offset consumer (assoc m :auto-offset-reset :earliest)))

(defn get-latest-topic-offset
  "Retuns the latest message offset in a partition. See get-topic-offset function for more info"
  [^SimpleConsumer consumer m]
  (get-topic-offset consumer (assoc m :auto-offset-reset :latest)))

(defn set-consumer-offset
  "Sets the stored zookeeper consumer offset to the provided consumer-offset value
   This offset is stored in the zookeeper location
         /consumers/<group-id>/offsets/<topic>/<partition-id>"

  [{:keys [zookeeper-connect group-id topic partition-id consumer-offset]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (set-offset! zookeeper-props group-id topic partition-id consumer-offset)))

(defn calculate-partition-backlog
  "Returns the partition \"backlog\" by querying the broker (associated to by the consumer) for it's last/latest
   message offset and subtracting the given consumer-offset from it.
   If there's any exception whilst querying the broker, -1 is returned and a warning message is logged."
  [consumer {:keys [consumer-offset] :as m}]
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
