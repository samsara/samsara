(ns qanal.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [qanal.kafka :as kafka]
            [qanal.elsasticsearch :as els]
            [validateur.validation :refer [validation-set nested inclusion-of presence-of compose-sets]])
  (:gen-class)
  (:import (kafka.common OffsetOutOfRangeException InvalidMessageSizeException)))


(def ^:private config-validator
  (compose-sets
    (nested :kafka-source (validation-set
                            (presence-of :zookeeper-connect)
                            (presence-of :connect-retry)
                            (presence-of :group-id)
                            (presence-of :topic)
                            (presence-of :partition-id)
                            (inclusion-of :auto-offset-reset :in #{:earliest :latest})
                            (presence-of :fetch-size)))
    (nested :elasticsearch-target (validation-set
                                    (presence-of :end-point)))))


(defn valid-config? [c]
  (let [errors (config-validator c)]
    (if (empty? errors)
      c
      (log/warn "The configuration file has invalid values/structure : " errors))))

(def known-options
  [
   ["-c" "--config CONFIG" "Configuration File"
    :parse-fn str]
   ])

(defn parse-opt-errors->str [errors]
  (str "There was an error in the command line : \n" (clojure.string/join \newline errors)))

(defn read-config-file [file-name]
  (when file-name
    (log/info "Reading config file : " file-name)
    (edn/read-string (slurp file-name))))

(defn exit [exit-code msg]
  (println msg)
  (System/exit exit-code))

(defn- sleep [millis]
  (try
    (Thread/sleep millis)
    (catch InterruptedException ie
      (log/warn "Sleeping Thread was interrupted : " ie))))

;; TODO yuck! need to come up with a better name
(defn- result-or-exception [f & args]
  (try
    (apply f args)
    (catch Exception e
      e)))

;; TODO Need to refactor the following functions as they are very similar
(defn- connect-to-kafka [m]
  (log/info "Using Zookeeper cluster [" (:zookeeper-connect m) "] to find lead broker")
  (let [c (result-or-exception kafka/connect-to-lead-broker m)
        retry (:connect-retry m)]
    (if-not (instance? Exception c)
      c
      (do
        (log/warn "Unable to connect to the Kafka Cluster due to this Exception : " c)
        (log/warn "Will retry in " retry " millisseconds")
        (sleep retry)
        (recur m)))))

(defn- apply-consumer-offset
  "Takes a map and uses the :zookeeper-connect value to connect to Zookeeper,
   gets the consumer offset that is associated to the given group-id
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [m]
  (let [current-offset (result-or-exception kafka/get-consumer-offset m)
        retry (:connect-retry m)]
    (if-not (instance? Exception current-offset)
      (assoc m :offset current-offset)
      (do
        (log/warn "Unable to get the consumer offset, from Zookeeper, due to this Exception : " current-offset)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur m)))))

(defn- apply-topic-offset
  "Takes a SimpleConsumer and a map. It then uses the consumer to connect to the Broker and
   gets either the earliest or latest offset for the :topic and :partition-id keys (in provided map)
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [consumer m]
  (let [current-offset (result-or-exception kafka/get-topic-offset consumer m)
        retry (:connect-retry m)]
    (if-not (instance? Exception current-offset)
      (assoc m :offset current-offset)
      (do
        (log/warn "Unable to get the topic/partition offset, fron Kafka, due to this Exception : " current-offset)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur consumer m)))))

(defn- set-consumer-offset
  "Takes a map and uses the :zookeeper-connect :topic :partition-id and :offset values to set the
   consumer offset within zookeeper. It returns either a map (when updating offset) or string
   (when creating new offset)"
  [m]
  (let [zk-result (result-or-exception kafka/set-consumer-offset m)
        retry (:connect-retry m)]
    (if-not (instance? Exception zk-result)
      zk-result
      (do
        (log/warn "Unable to set the zookeeper consumer offset, due to this Exception : " zk-result)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur m)))))


(defn- apply-initial-offset
  "Takes a SimpleConsumer and a map. It will first try to apply the Zookeeper's consumer offset to the
   map. If this value (:offset) is nil, which signifies that the consumer offset is not existant,
   it will then get either the :earliest or :latest offset of the actual topic-partition from the
   partition's lead broker"
  [consumer m]
  (let [m-consumer-offset (apply-consumer-offset m)
        consumer-offset (:offset m-consumer-offset)
        offset-reset (:auto-offset-reset m-consumer-offset)]
    (if (nil? consumer-offset)
      (do
        (log/info "No Existing Consumer offset found, using " offset-reset " topic/partition offset" )
        (let [m-reset-offset (apply-topic-offset consumer m)]
          (log/info "Offset set to RESET offset of [" (:offset m-reset-offset) "]")
          m-reset-offset))
      m-consumer-offset)))



(defn- get-kafka-messages [consumer m]
  (let [msg-seq (result-or-exception kafka/get-messages consumer m)
        retry (:connect-retry m)
        offset-reset (:auto-offset-reset m)]
    (if-not (instance? Exception msg-seq)
      msg-seq
      (do
        (log/warn "Unable to get kafka messages due to this Exception : " msg-seq)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (cond (instance? OffsetOutOfRangeException msg-seq)
              (do
                (log/warn "OffsetOutOfRangeException was encountered, will use the " offset-reset " topic/partiion offset")
                (recur consumer (apply-topic-offset consumer m)))
              (instance? InvalidMessageSizeException msg-seq)
              (do
                (log/warn "InvalidMessageSizeException was encountered, will use the " offset-reset " topic/partition offset")
                (recur consumer (apply-topic-offset consumer m)))
              :else
              (do
                (log/warn "An unexpected Exception was encountered whilst getting kafka messages. Reconnecting Kafka and trying again")
                (recur (connect-to-kafka m) m)))))))

(defn siphon [{:keys [kafka-source elasticsearch-target]}]
  (loop [c (connect-to-kafka kafka-source)
         source-with-offset (apply-initial-offset c kafka-source)]
    (let [msgs-seq (get-kafka-messages c source-with-offset)]
      (if (empty? msgs-seq)
        (do
          (sleep 1000)
          (recur c source-with-offset))
        (let [bulk-stats (els/bulk-index msgs-seq elasticsearch-target)
              last-offset (:last-offset bulk-stats)
              m-with-offset (assoc source-with-offset :offset (inc last-offset))]
          (set-consumer-offset m-with-offset)
          (recur c m-with-offset))))))

(defn -main [& args]
  (let [{:keys [options errors ]} (parse-opts args known-options)
        config-file (:config options)]
    (when errors
      (exit 1 (parse-opt-errors->str errors)))
    (when (nil? config-file)
      (exit 2 "Please supply a configuration file via -c option"))
    (let [cfg (read-config-file config-file)]
      (when-not (valid-config? cfg)
        (exit 3 "Please fix the configuration file"))
      (siphon cfg))))



(comment
  (def test-config {:kafka-source {:zookeeper-connect  "localhost:49157"
                                   :connect-retry      5000
                                   :group-id           "Qanal"
                                   :topic "river"
                                   :partition-id 0
                                   :auto-offset-reset  :earliest ; Can only be earliest or latest
                                   :fetch-size         (* 10 1024 1024)}
                    :elasticsearch-target {:end-point "http://localhost:9200"}})
  (siphon test-config)
  )