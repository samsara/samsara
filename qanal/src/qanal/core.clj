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
(ns qanal.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.rotor :as rotor]
            [qanal.kafka :as kafka]
            [qanal.elsasticsearch :as els]
            [qanal.utils :refer [sleep exit execute-if-elapsed result-or-exception continously-try]]
            [qanal.metrics :as metrics]
            [validateur.validation :refer [validation-set nested inclusion-of presence-of compose-sets]])
  (:gen-class)
  (:import (kafka.common OffsetOutOfRangeException InvalidMessageSizeException)))

(log/set-config! [:appenders :rotor] {:min-level :info
                                      :enabled?  true
                                      :async? false ; should be always false for rotor
                                      :fn rotor/appender-fn
                                      })
(def ^:private default-rotor-config {:level :info :path "qanal.log" :max-size (* 10 1024) :backlog 10})

(def ^:private known-options
  [
   ["-c" "--config CONFIG" "Configuration File"
    :parse-fn str]
   ])

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


(defn- record-stats [kafka-consumer {:keys [topic partition-id consumer-offset kafka-msgs-count bulked-docs bulked-time]
                                     :or {kafka-msgs-count 0 bulked-docs 0 bulked-time 0}
                                     :as stats}]
  ;(log/debug "Stats ->" stats)
  (let [kafka-args {:topic topic :partition-id partition-id :consumer-offset consumer-offset}
        gauge-fn-map {:backlog-fn       (fn [] (kafka/calculate-partition-backlog kafka-consumer kafka-args))}
        rates-map {:msgs kafka-msgs-count
                   :bulked-docs bulked-docs
                   :bulked-time bulked-time}]

    (metrics/record-qanal-counters stats)
    (metrics/record-qanal-gauges topic partition-id gauge-fn-map)
    (metrics/record-qanal-rates topic partition-id rates-map)
    (metrics/sensibly-log-stats)))


(defn- create-default-stats [{:keys [topic partition-id consumer-offset kafka-msgs-count bulked-docs bulked-time]
                              :or {kafka-msgs-count 0 bulked-docs 0 bulked-time 0}}]
  {:topic topic :partition-id partition-id :consumer-offset consumer-offset :kafka-msgs-count kafka-msgs-count
   :bulked-docs bulked-docs :bulked-time bulked-time})


(defn- apply-logging-options [logging-options]
  (let [log-level (:min-level logging-options)
        appender-config (dissoc logging-options :min-level)]
    (log/set-config! [:appenders :rotor :min-level] log-level)
    (log/set-config! [:shared-appender-config :rotor] appender-config)))

(defn valid-config? [c]
  (let [errors (config-validator c)]
    (if (empty? errors)
      c
      (log/warn "The configuration file has invalid values/structure : " errors))))


(defn parse-opt-errors->str [errors]
  (str "There was an error in the command line : \n" (clojure.string/join \newline errors)))

(defn read-config-file [file-name]
  (when file-name
    (log/info "Reading config file : " file-name)
    (edn/read-string (slurp file-name))))


(defn connect-to-kafka [{:keys [zookeeper-connect topic partition-id connect-retry] :as m}]
  (log/info "Using Zookeeper cluster [" zookeeper-connect "] to connect to lead broker for topic["
            topic "] partition-id[" partition-id "]")
  (let [error-msg "Unable to connect to Kafka Cluster"
        c (continously-try kafka/connect-to-lead-broker [m] connect-retry error-msg)]
    (if (some? c)
      c
      (do
        (log/warn "Will try again to connect to the lead broker for topic[" topic "] partition-id[" partition-id "]")
        (sleep connect-retry)
        (recur m)))))

(defn apply-consumer-offset
  "Takes a map and uses the :zookeeper-connect value to connect to Zookeeper,
   gets the consumer offset that is associated to the given group-id
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [m]
  (let [retry (:connect-retry m)
        error-msg "Unable to get consumer offset from Zookeeper"
        current-consumer-offset (continously-try kafka/get-consumer-offset [m] retry error-msg)]
    (assoc m :consumer-offset current-consumer-offset)))

(defn apply-topic-offset
  "Takes a SimpleConsumer and a map. It then uses the consumer to connect to the Broker and
   gets either the earliest or latest offset for the :topic and :partition-id keys (in provided map)
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [consumer m]
  (let [retry (:connect-retry m)
        args [consumer m]
        error-msg (str "Unable to get the offset for topic->" (:topic m) " partition->" (:partition-id m) " from Kafka")
        current-partition-offset (continously-try kafka/get-topic-offset args retry error-msg)]
    (assoc m :consumer-offset current-partition-offset)))

(defn set-consumer-offset
  "Takes a map and uses the :zookeeper-connect :topic :partition-id and :offset values to set the
   consumer offset within zookeeper. It returns either a map (when updating offset) or string
   (when creating new offset)"
  [m]
  (let [retry (:connect-retry m)
        error-msg (str "Unable to set Zookeeper Consumer offset for topic->" (:topic m) " partition->" (:partition-id m))]
    (continously-try kafka/set-consumer-offset [m] retry error-msg)))


(defn apply-initial-offset
  "Takes a SimpleConsumer and a map. It will first try to apply the Zookeeper's consumer offset to the
   map. If this value (:offset) is nil, which signifies that the consumer offset is not existant,
   it will then get either the :earliest or :latest offset of the actual topic-partition from the
   partition's lead broker"
  [consumer m]
  (let [m-consumer-offset (apply-consumer-offset m)
        consumer-offset (:consumer-offset m-consumer-offset)
        {:keys [topic partition-id auto-offset-reset]} m]
    (if (nil? consumer-offset)
      (do
        (log/warn "No Existing Zookeeper Consumer offset found for topic->" topic
                  " partition-id->" partition-id)
        (let [m-reset-offset (apply-topic-offset consumer m)]
          (log/info "Using auto-offset-reset->" auto-offset-reset " to set Consumer Offset for topic->" topic
                    " partition-id->" partition-id " to [" (:consumer-offset m-reset-offset) "]")
          m-reset-offset))
      (do
        (log/info "Topic->" topic " Partition-id->" partition-id "Using Zookeeper Consumer offset->" consumer-offset)
        m-consumer-offset))))



(defn get-kafka-messages [consumer state]
  (let [msg-seq (result-or-exception kafka/get-messages consumer state)
        retry (:connect-retry state)
        offset-reset (:auto-offset-reset state)]
    (if-not (instance? Exception msg-seq)
      msg-seq
      (do
        (log/warn "Unable to get kafka messages due to this Exception : " msg-seq)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (cond (instance? OffsetOutOfRangeException msg-seq)
              (do
                (log/warn "OffsetOutOfRangeException was encountered, will use the " offset-reset " topic/partiion offset")
                (recur consumer (apply-topic-offset consumer state)))
              (instance? InvalidMessageSizeException msg-seq)
              (do
                (log/warn "InvalidMessageSizeException was encountered, will use the " offset-reset " topic/partition offset")
                (recur consumer (apply-topic-offset consumer state)))
              :else
              (do
                (log/warn "An unexpected Exception was encountered whilst getting kafka messages. Reconnecting Kafka and trying again")
                (recur (connect-to-kafka state) state)))))))


(defn siphon [{:keys [kafka-source elasticsearch-target riemann-host logging-options]
               :or {logging-options default-rotor-config}}]
  (metrics/connect-to-riemann riemann-host)
  (apply-logging-options logging-options)
  (loop [consumer (connect-to-kafka kafka-source)
         state (apply-initial-offset consumer kafka-source)]
    (let [kafka-msgs (get-kafka-messages consumer state)
          stats (create-default-stats state)]
      (if (empty? kafka-msgs)
        (do
          (record-stats consumer stats)
          (sleep 1000)
          (recur consumer state))

        (let [els-stats (els/bulk-index elasticsearch-target kafka-msgs)
              last-msg-offset (:kafka-msgs-last-offset els-stats)
              consumer-offset (inc last-msg-offset)
              updated-stats (merge stats els-stats {:consumer-offset consumer-offset})
              updated-state (assoc state :consumer-offset consumer-offset)]
          (set-consumer-offset updated-state)
          (record-stats consumer updated-stats)
          (recur consumer updated-state))))))


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
                                   :group-id           "qanal"
                                   :topic "river"
                                   :partition-id 0
                                   :auto-offset-reset  :earliest ; Can only be earliest or latest
                                   :fetch-size         (* 10 1024 1024)}
                    :elasticsearch-target {:end-point "http://localhost:9200"}})
  (siphon test-config)
  )
