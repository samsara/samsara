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
            [validateur.validation :refer [validation-set nested inclusion-of presence-of compose-sets]])
  (:gen-class)
  (:import (kafka.common OffsetOutOfRangeException InvalidMessageSizeException)))

(log/set-config! [:appenders :rotor] {:min-level :info
                                      :enabled?  true
                                      :async? false ; should be always false for rotor
                                      :fn rotor/appender-fn
                                      })
(def ^:private default-rotor-config {:level :info :path "Qanal.log" :max-size (* 10 1024) :backlog 10})

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

(defn- execute-if-elapsed [{:keys [last-time-executed] :as m} window f & args]
  (let [last-time (or last-time-executed 0)
        elapsed (- (System/currentTimeMillis) last-time)]
    #_(log/debug "last-time->" last-time " elapsed->" elapsed)
    (if (> elapsed window)
      (do
        (apply f args)
        (assoc m :last-time-executed (System/currentTimeMillis)))
      m)))

;; TODO yuck! need to come up with a better name
(defn- result-or-exception [f & args]
  (try
    (apply f args)
    (catch Exception e
      e)))

(defn- continously-try [f args retry error-msg]
  (let [result (result-or-exception f args)]
    (if-not (instance? Exception result)
      result
      (do
        (log/warn error-msg)
        (log/warn "Exception : " result)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur f args retry error-msg)))))

(defn- connect-to-kafka [m]
  (log/info "Using Zookeeper cluster [" (:zookeeper-connect m) "] to find lead broker")
  (continously-try kafka/connect-to-lead-broker m (:connect-retry m) "Unable to connect to Kafka Cluster"))

(defn- apply-consumer-offset
  "Takes a map and uses the :zookeeper-connect value to connect to Zookeeper,
   gets the consumer offset that is associated to the given group-id
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [m]
  (let [retry (:connect-retry m)
        error-msg "Unable to get consumer offset from Zookeeper"
        current-consumer-offset (continously-try kafka/get-consumer-offset m retry error-msg)]
    (assoc m :offset current-consumer-offset)))

(defn- apply-topic-offset
  "Takes a SimpleConsumer and a map. It then uses the consumer to connect to the Broker and
   gets either the earliest or latest offset for the :topic and :partition-id keys (in provided map)
   and then returns the given map with the offset associated with key :offset.
   Note: if there is no consumer offset, :offset will have nil value"
  [consumer m]
  (let [retry (:connect-retry m)
        args [consumer m]
        error-msg (str "Unable to get the offset for topic->" (:topic m) " partition->" (:partition-id m) " from Kafka")
        current-partition-offset (continously-try kafka/get-topic-offset args retry error-msg)]
    (assoc m :offset current-partition-offset)))

(defn- set-consumer-offset
  "Takes a map and uses the :zookeeper-connect :topic :partition-id and :offset values to set the
   consumer offset within zookeeper. It returns either a map (when updating offset) or string
   (when creating new offset)"
  [m]
  (let [retry (:connect-retry m)
        error-msg (str "Unable to set Zookeeper Consumer offset for topic->" (:topic m) " partition->" (:partition-id m))]
    (continously-try kafka/set-consumer-offset m retry error-msg)))


(defn- apply-initial-offset
  "Takes a SimpleConsumer and a map. It will first try to apply the Zookeeper's consumer offset to the
   map. If this value (:offset) is nil, which signifies that the consumer offset is not existant,
   it will then get either the :earliest or :latest offset of the actual topic-partition from the
   partition's lead broker"
  [consumer m]
  (let [m-consumer-offset (apply-consumer-offset m)
        consumer-offset (:offset m-consumer-offset)]
    (if (nil? consumer-offset)
      (do
        (log/warn "No Existing Zookeeper Consumer offset found for topic->" (:topic m)
                  " partition-id->" (:partition-id m))
        (let [m-reset-offset (apply-topic-offset consumer m)]
          (log/info "Using auto-offset-reset to set Consumer Offset for topic->" (:topic m) " partition-id->"
                    (:partition-id m) " to [" (:offset m-reset-offset) "]")
          m-reset-offset))
      (do
        (log/info "Topic->" (:topic m) " Partition-id->" (:partition-id m) "Using Zookeeper Consumer offset->" consumer-offset)
        m-consumer-offset))))



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


(defn- print-stats [c {:keys [topic partition-id offset received-msgs bulked-docs bulked-time]}]
  (let [kafka-args {:topic topic :partition-id partition-id :auto-offset-reset :latest}
        current-offset (result-or-exception kafka/get-topic-offset c kafka-args)
        backlog (if (instance? Exception current-offset) "EXCEPTION" (- current-offset offset))]

    (log/info "Topic->" topic " Partition-id->" partition-id " Received Msgs->" received-msgs
              " Bulked Docs->" bulked-docs " Bulked-time->" bulked-time "ms"
              " Backlog->" backlog)))

(defn- apply-logging-options [logging-options]
  (let [log-level (:min-level logging-options)
        appender-config (dissoc logging-options :min-level)]
    (log/set-config! [:appenders :rotor :min-level] log-level)
    (log/set-config! [:shared-appender-config :rotor] appender-config)))

(defn siphon [{:keys [kafka-source elasticsearch-target logging-options]
               :or {logging-options default-rotor-config}}]
  (apply-logging-options logging-options)
  (loop [c (connect-to-kafka kafka-source)
         source-with-offset (apply-initial-offset c kafka-source)]
    (let [msgs-seq (get-kafka-messages c source-with-offset)]
      (if (empty? msgs-seq)
        (let [print-stats-sensibly (partial execute-if-elapsed source-with-offset 1000 print-stats c)
              source-with-offset (print-stats-sensibly (assoc source-with-offset :received-msgs 0 :bulked-docs 0 :bulked-time 0))]
          (sleep 1000)
          (recur c source-with-offset))
        (let [bulk-stats (els/bulk-index msgs-seq elasticsearch-target)
              last-offset (:last-offset bulk-stats)
              m-with-offset (assoc source-with-offset :offset (inc last-offset))]
          (set-consumer-offset m-with-offset)
          (let [print-stats-sensibly (partial execute-if-elapsed m-with-offset 1000 print-stats c)
                m-with-offset (print-stats-sensibly (merge m-with-offset bulk-stats))]
            (recur c m-with-offset)))))))

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