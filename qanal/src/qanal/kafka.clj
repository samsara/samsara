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
            [clj-kafka.zk :refer (brokers committed-offset set-offset!) :as zk]
            [clj-kafka.core :refer (ToClojure)]
            [clj-kafka.consumer.simple :refer (consumer topic-meta-data messages topic-offset)]
            [schema.core :as sk]
            [clojure.string :as st]
            [samsara.trackit :refer [track-time]]
            [taoensso.timbre :as log])
  (:require [clojure.core.async :as async :refer [<!! >!! timeout chan alt!!]])
  (:require [clj-kafka.consumer.simple :as zs]))



(def kafka-state (atom {}))

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



(defn from-json-safe
  "Json parsing with exception handling"
  [^String m]
  (safe-short nil (str "Unable to parse this json message |" m "|")
        (from-json m)))


(def validate-river-format
  "Validation schema for incoming messages"
  {(sk/required-key :index)  sk/Str
   (sk/required-key :type)   sk/Str
   (sk/optional-key :id)     sk/Str
   (sk/required-key :source) {sk/Any sk/Any}})


(defn validate-message
  "Validate format of incoming message"
  [msg]
  (when msg
    (safe-short nil (str "Invalid message format: " (prn-str msg))
          (sk/validate validate-river-format msg))))


(defn unmarshall-values
  "Decodes json messages into clojures maps, or set it to nil if it can't"
  [msg]
  (update-in msg [:value] (comp validate-message from-json-safe bytes->string)))


(defn get-messages
  "Uses the provided arguments to consume kafka messages and return the messages in a lazy sequence.
   The lazy sequence is contains messages of the structure
   (defrecord KafkaMessage [topic offset partition key value])
   topic --> Kafka Topic
   offset --> Offset in the partition
   partition --> partitionid
   key --> Key used to choose partition this message was sent to
   value --> a Clojure map representing the message"
  [^SimpleConsumer consumer
   {:keys [group-id topic partition-id consumer-offset fetch-size]}]
  (track-time (str "qanal.kafka.fetch-messages." topic "." partition-id)
     (doall
      (map unmarshall-values               ;; conv bytes to clojure maps
           (messages consumer group-id     ;; fetch messages
                     topic partition-id
                     consumer-offset fetch-size)))))


(defn get-consumer-offset
  "Returns the stored consumer offset in zookeeper for the provided group-id.
   This offset is stored in the zookeeper location
           /consumers/<group-id>/offsets/<topic>/<partition-id>
   If no offset is found (i.e location is not found) then nil is returned "
  [{:keys [zookeeper-connect group-id topic partition-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    ;; TODO: committed-offset opens/close connection on every request
    ;; this need to change if you have to check 50-100 partitions.
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



(comment

  (def cfg {"zookeeper.connect" "docker:49153"})

  ;; lead broker
  (zk/controller cfg)
  ;; brokers list
  (zk/brokers cfg)
  (zk/broker-list (zk/brokers cfg))
  ;; list of topics
  (zk/topics cfg)
  ;; partitions and broker ids
  (zk/partitions cfg "events")

  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;




(defn index-by [extractor seq]
  (into {} (map (juxt extractor identity) seq)))

(defn- get-topics-metadata
  "it connects to a random broker and fetches the partition metadata as a map
  of topics and :partition-metadata. For every topic, all partitions are listed.
  For every partition the :leader broker, the :replicas brokers and all
  the :in-sync-replicas brokers are listed with their :broker-id, :host, :port
  and :connection string.

  For example the following topic `sample-topic` has 2 partitions and replication factor of 3.


      {\"sample-topic\"
       {:topic \"sample-topic\",
        :partition-metadata
        {0
         {:partition-id 0,
          :leader {:connect \"192.168.59.103:49156\", :host \"192.168.59.103\", :port 49156, :broker-id 1},
          :replicas ({...} {...} {...}),
          :in-sync-replicas ({...} {...} {...}),
          :error-code 0}}
        {1
         {:partition-id 1,
          :leader {:connect \"192.168.59.111:49156\", :host \"192.168.59.111\", :port 49156, :broker-id 2},
          :replicas ({...} {...} {...}),
          :in-sync-replicas ({...} {...} {...}),
          :error-code 0}}}}


  Usage example:

      ;; to get the topics metadata run
      (def data (get-topics-metadata zk-cfg [ \"sample-topic\", \"another-topic\" ])

      ;; to get the leader broker connection string for the partition 1 of sample-topic
      (get-in data [\"sample-topic\" :partition-metadata 1 :leader :connect])
      ;=> \"192.168.59.111:49156\"
  "
  [config topics]
  (let [{:keys [host port] :as broker} (rand-nth (zk/brokers config))]
    (with-open [consumer (zs/consumer host port (get config :group-id "metadata-consumer"))]
      (->>
       (zs/topic-meta-data consumer topics)
       (map #(update-in % [:partition-metadata] (partial index-by :partition-id)))
       (index-by :topic)))))



(defn set-zk-connection! [state zk-connection]
  (swap! state assoc :zk zk-connection))

(defn set-topics-scheme! [state topics]
  (swap! state assoc :topics topics))


(defn refresh-partitions
  "Update the partitions metadata"
  [{:keys [zk topics] :as state}]
  (assoc state :metadata (get-topics-metadata zk (map first topics))))

(defn- lead-brokers
  "return a list of maps of lead brokers descriptors"
  [metadata]
  (->> metadata
       (mapcat (comp :partition-metadata second))
       (map (comp :leader second))
       (index-by :broker-id) ;; dedup
       (map second)))


(defn- reuse-lead-brokers-connecitons
  "From the metadata extracts the list of `new` lead brokers
  and when a connection is already present it will be reused.
  it generate"
  [group-id metadata old-brokers]
  (let [brokers (lead-brokers metadata)]
    (->> brokers
         (map (fn [{:keys [broker-id host port] :as broker}]
                (assoc broker :consumer
                       (or (get-in old-brokers [broker-id :consumer])
                           (consumer host port group-id)))))
         (index-by :broker-id))))


(defn refresh-brokers
  "It refresh the list of lead brokers reusing
  the connection where possible. If connection
  wasn't present a new connection is created."
  [{:keys [metadata zk] :as state}]
  (update-in state
             [:brokers]
             (partial reuse-lead-brokers-connecitons
                      (:group-id zk)
                      metadata)))



(defn real-partitions
  "Given a topic and the metadata returns the real set of partitions,
  or empty set (#{}) if the topic doesn't exist."
  [topic metadata]
  (set
   (map first
        (get-in metadata [topic :partition-metadata] {}))))


(defn wanted-partitions
  "Given a topic and a configuration of partitions it returns
  a set of partitions to process (wanted)

  Example:

  (wanted-partitions \"topic-with-5-partitions\" :all metadata)
  ;=> #{0 1 2 3 4}

  (wanted-partitions \"topic-with-5-partitions\" [0 3 67] metadata)
  ;=> #{0 3}

  (wanted-partitions \"not-existing-topic\" [0 3] metadata)
  ;=> #{}
  "
  [topic parts metadata]
  (let [real-parts (real-partitions topic metadata)]
    (if (= :all parts)
      real-parts
      (set (filter real-parts parts)))))


(defn- topic-fetching-spec
  "Build the fetching specification for a topic and configured partitions.
  It returns a list of maps with almost all required configuration
  `get-messages` call."
  [topic parts {:keys [group-id fetch-size]} metadata ]
   (let [parts-to-process (wanted-partitions topic parts metadata)]
     (map (fn [p] {:group-id group-id
                  :topic topic
                  :partition p
                  :fetch-size fetch-size}) parts-to-process)))

(defn fetching-specs
  [topics-conf cfg metadata]
  (index-by
   (comp (partial st/join "/") (juxt :topic :partition))
   (mapcat
    (fn [[topic parts]]
      (topic-fetching-spec topic parts cfg metadata))
    topics-conf)))

(defn refresh-fetching-specs [{:keys [topics zk metadata] :as state}]
  (update-in state [:fetching] (partial merge (fetching-specs topics zk metadata))))


;; TODO: REVIEW DOC-STRING
(defn- consumer-offsets!
  "Returns the stored consumer offset in zookeeper for the provided group-id.
   This offset is stored in the zookeeper location
           /consumers/<group-id>/offsets/<topic>/<partition-id>
   If no offset is found (i.e location is not found) then nil is returned "
  [cfg topics-and-partitions]
  ;; TODO: committed-offset opens/close connection on every request
  ;; this need to change if you have to check 50-100 partitions.
  ;; `committed-offset` need to accept a list of topic/partition pairs
  (into {}
        (map (fn [[k {:keys [group-id topic partition] :as m}]]
               [k (update-in m [:consumer-offset]
                            (fn [ov]
                              (let [ov (or ov -1 )
                                    nv (or (committed-offset cfg group-id topic partition) -1)]
                                (if (> ov nv) ov nv))))])
             topics-and-partitions)))


(defn refresh-consumer-offsets [{:keys [zk] :as state}]
  (update-in state [:fetching] #(consumer-offsets! zk %)))


(defn refresh-all-topics-metadata
  "It refreshes all metadata, keeps track when last update was made"
  [kafka-state]
  (-> kafka-state
      refresh-partitions
      refresh-brokers
      refresh-fetching-specs
      refresh-consumer-offsets
      (assoc-in [:last-update] (java.util.Date.))))


(defn commit-offsets!
  "it commits the consumed offsets to zookeeper and update the state"
  [state-atom]
  (let [{:keys [zk fetching] :as state} @state-atom

        committed
        (into {}
              (for [[k {:keys [group-id topic partition consumer-offset committed-offset] :as v}] fetching]
                [k
                 (if (> consumer-offset (or committed-offset -1))
                   (do
                     ;; TODO: this should accept a list of topics/partions/offsets
                     ;; TODO: what if this fails?
                     (set-offset! zk group-id topic partition consumer-offset)
                     (assoc v :committed-offset consumer-offset))
                   v)]))
        new-state (assoc state :fetching committed)]
    ;; if it doesn't happen not a big deal, the real values are
    ;; committed to zk, and they will be updated again
    ;; at the next round
    (compare-and-set! state-atom state new-state)))


(defmacro forever-do [name sleep & body]
  `(let [_name# ~name _sleep# ~sleep]
     (async/thread
       (while true
         (try
           ~@body
           (catch Exception x#
             (log/warn "ERROR: " _name# ", reason:" x# ", will retry after:" _sleep# "ms")))
         (when (> _sleep# 0)
           (Thread/sleep _sleep#))))))

(defmacro forever-go [name sleep & body]
  `(let [_name# ~name _sleep# ~sleep]
     (async/go
       (while true
         (try
           ~@body
           (catch Exception x#
             (log/warn "ERROR: " _name# ", reason:" x# ", will retry after:" _sleep# "ms")))
         (when (> _sleep# 0)
           (Thread/sleep _sleep#))))))


(defn refresh-state! [kafka-state]
  (swap! kafka-state refresh-all-topics-metadata))

(defn- start-metadate-update-thread []
  (forever-do "updating kafka metadata" 5000
              (refresh-all-topics-metadata! kafka-state)))




(comment

  (set-zk-connection! kafka-state {"zookeeper.connect" "docker:49153" :group-id "some-consumer-id" :fetch-size 1024000})
  (set-topics-scheme! kafka-state {"test1" :all "test3" [0 2]})

  (refresh-all-topics-metadata! kafka-state)

  (start-metadate-update-thread)

  kafka-state

  (commit-offsets! kafka-state)

  )
