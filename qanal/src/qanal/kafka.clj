(ns qanal.kafka
  (:import (kafka.javaapi.consumer SimpleConsumer))
  (:require [clj-kafka.zk :refer (brokers committed-offset set-offset!)]
            [clj-kafka.core :refer (ToClojure)]
            [clj-kafka.consumer.simple :refer (consumer topic-meta-data messages topic-offset)]
            [clojure.core.async :refer (buffer chan >!! thread close!)]
            [clojure.tools.logging :as log]))


;; TODO this should be part of clj-kafka.core namespace
;; will remove once pull request (https://github.com/pingles/clj-kafka/pull/40) is accepted
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

(defn connect-to-broker [{:keys [topic partition-id zookeeper-connect group-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}
        kvs [:topic topic :partition-id partition-id :group-id group-id]
        brokers-seq (map #(apply assoc % kvs) (brokers zookeeper-props))
        lead-broker (some get-lead-broker brokers-seq)]
    (log/debug "Zookeeper cluster[" zookeeper-connect "] provided these kafka brokers ["
               brokers-seq "]")
    (if lead-broker
      (do
        (log/info "Broker[" lead-broker "] is the lead broker for topic[" topic "] partition["
                  partition-id "]")
        (consumer (:host lead-broker) (:port lead-broker) group-id))
      (log/warn "Lead Broker NOT found for topic[" topic "] partition-id[" partition-id "] !!"))))


(defn get-messages [^SimpleConsumer consumer {:keys [group-id topic partition-id offset fetch-size]}]
  (messages consumer group-id topic partition-id offset fetch-size))

(defn get-consumer-offset [{:keys [zookeeper-connect group-id topic partition-id]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (committed-offset zookeeper-props group-id topic partition-id)))

(defn get-topic-offset [^SimpleConsumer consumer {:keys [topic partition-id auto-offset-reset]}]
  {:pre [(#{:earliest :latest} auto-offset-reset)]}
  (topic-offset consumer topic partition-id auto-offset-reset))

(defn set-consumer-offset [{:keys [zookeeper-connect group-id topic partition-id offset]}]
  (let [zookeeper-props {"zookeeper.connect" zookeeper-connect}]
    (set-offset! zookeeper-props group-id topic partition-id offset)))

(comment

  (def con (connect-to-broker {:topic "river" :partition-id 0 :zookeeper-connect "localhost:49155" :group-id "someclient"}))
  (->>
    (get-messages con {:group-id "someclient" :topic "river" :partition-id 0 :offset 0 :fetch-size (* 10 1024 1024)})
    (map #(-> % :value String. println)))
  )
