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
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.bulk :as esb])
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.rotor :as rotor]
            [qanal.kafka :as kafka]
            [qanal.elasticsearch :as els]
            [qanal.utils :refer :all]
            [samsara.trackit :as trackit]
            [schema.core :as s]
            [clojure.java.io :as io])
  (:gen-class)
  (:import (kafka.common OffsetOutOfRangeException InvalidMessageSizeException)))


(def DEFAULT-CONFIG
  {:kafka-source
   {:connect-retry      5000
    :group-id           "qanal"
    :auto-offset-reset  :earliest       ; Can only be earliest or latest
    :fetch-size         10485760        ;size in bytes (10mb)
    }

   :elasticsearch-target {:end-point "http://localhost:9200"}

   :tracking {:enabled false
              :prefix "samsara.qanal" }

   :logging-options {:min-level :info
                     :path "/tmp/qanal.log"    ;full path name for the file
                     :max-size 10485760        ;size in bytes (10mb)
                     :backlog 10}})


(def ^:private default-rotor-config
  {:level :info :path "qanal.log" :max-size (* 10 1024 1024) :backlog 10})


(def ^:private known-options
  [
   ["-c" "--config CONFIG" "Configuration File"
    :validate [#(.exists (io/file %)) "The given file must exist"]]
   ])

;; TODO: does the schema need to be so strict??
(def ^:private config-schema
  {:kafka-source {:zookeeper-connect  s/Str
                  :connect-retry      s/Int
                  :group-id           s/Str
                  :auto-offset-reset  (s/enum :earliest :latest)
                  :fetch-size         s/Int
                  }
   ;; TODO: check correct schema rule for OR
   :topics {s/Str (or s/Keyword [s/Int])}
   :elasticsearch-target {:end-point s/Str}
   :tracking {s/Keyword s/Any}
   :logging-options {:min-level (s/enum :trace :debug :info :warn :error)
                     :path s/Str
                     :max-size s/Int
                     :backlog s/Int}})




(defn- apply-logging-options [logging-options]
  (let [log-level (:min-level logging-options)
        appender-config (dissoc logging-options :min-level)]
    (log/set-config! [:appenders :rotor :min-level] log-level)
    (log/set-config! [:shared-appender-config :rotor] appender-config)))


(defn parse-opt-errors->str [errors]
  (str "There was an error in the command line : \n" (clojure.string/join \newline errors)))

(defn read-config-file [file-name]
  (when file-name
    ;; this is executed before the log is initialised
    (println "Reading config file : " file-name)
    (->> (edn/read-string (slurp file-name))
         (merge-with merge DEFAULT-CONFIG))))


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
   Note: if there is no consumer offset, :consumer-offset will have nil value"
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


;;
;; TODO: this is ugly
;;
(defn get-kafka-messages [consumer state]
  (let [msg-seq (result-or-exception kafka/get-messages consumer state)
        retry (:connect-retry state)
        offset-reset (:auto-offset-reset state)]
    (if-not (instance? Exception msg-seq)
      msg-seq
      (do
        (log/warn msg-seq "Unable to get kafka messages due to this exception!" )
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (cond (instance? OffsetOutOfRangeException msg-seq)
              (do
                (log/warn msg-seq "OffsetOutOfRangeException was encountered, will use the "
                          offset-reset " topic/partiion offset")
                (recur consumer (apply-topic-offset consumer state)))
              (instance? InvalidMessageSizeException msg-seq)
              (do
                (log/warn msg-seq "InvalidMessageSizeException was encountered, will use the "
                          offset-reset " topic/partition offset")
                (recur consumer (apply-topic-offset consumer state)))
              :else
              (do
                (log/warn msg-seq "An unexpected Exception was encountered whilst getting"
                          " kafka messages. Reconnecting Kafka and trying again")
                (recur (connect-to-kafka state) state)))))))

;;
;; TODO: nice concept having a central loop,
;; and now that some of the not relevant stuff
;; have been moved out is a bit cleaner,
;; but still not clear enough...
;; NEED CLEANER loop
;;
(defn siphon [{:keys [kafka-source elasticsearch-target]}]
  (loop [consumer (connect-to-kafka kafka-source)
         state (apply-initial-offset consumer kafka-source)]
    (let [kafka-msgs (get-kafka-messages consumer state)]
      (if (empty? kafka-msgs)

        (do
          (sleep 1000)
          (recur consumer state))

        ;; here we filter the invalida messages out,
        ;; and pass only the `good-messages` to elasticsearch
        ;; but we use the overall `kafka-msgs` to save last offset
        ;; to avoid continuously reading a failing msg.
        (let [good-messages (filter identity (map :value kafka-msgs))
              _ (els/bulk-index elasticsearch-target good-messages)
              next-consumer-offset (-> kafka-msgs last :offset inc)
              updated-state (assoc state :consumer-offset next-consumer-offset)]
          ;; TODO: don't like this as it won't
          ;; consume more msgs until the check point is saved
          ;; this would be better off in a separate thread
          (set-consumer-offset updated-state)
          (recur consumer updated-state))))))


(defn uber-siphon
  "It spawns a consumer thread for a specific topic/partion."
  [{:keys [kafka-source topics] :as config}]
  (let [zk (assoc kafka-source "zookeeper.connect" (:zookeeper-connect kafka-source))
        topics-spec (kafka/list-partitions-to-fetch topics zk)
        all-configs (map (fn [[topic partition]]
                           (-> config
                               (assoc-in [:kafka-source :topic] topic)
                               (assoc-in [:kafka-source :partition-id] partition)
                               (assoc-in [:cfg-name] (str topic "/" partition))))
                         topics-spec)]
    ;;
    ;; Consuming each partition in a separate thread..
    ;;
    (doseq [{:keys [kafka-source cfg-name] :as cfg} all-configs]
      (log/info "Starting consumer thread for" cfg-name)
      (kafka/forever-do (str "consuming partition: " cfg-name) (:connect-retry kafka-source)
                        (siphon cfg)))))



(defn- init-log! [config]
  (log/set-config! [:timestamp-pattern]
                   "yyyy-MM-dd HH:mm:ss.SSS zzz")

  (log/set-config! [:appenders :rotor]
                   {:min-level :info
                    :enabled?  true
                    :async? false     ; should be always false for rotor
                    :fn rotor/appender-fn})

  (apply-logging-options (or config default-rotor-config)))



(defn- init-tracking!
  "Initialises the metrics tracking system"
  [{enabled :enabled :as cfg}]
  ;; (trackit/start-reporting! {:type :console :reporting-frequency-seconds 30})
  (when enabled
    (log/info "Sending metrics to:" cfg)
    (trackit/set-base-metrics-name! "samsara" "qanal")
    (trackit/start-reporting! cfg)))


(defn init! [config]
  (init-log!      (:logging-options config))
  (init-tracking! (:tracking config)))



(defn -main [& args]
  (let [{:keys [options errors ]} (parse-opts args known-options)
        config-file (:config options)]
    (when errors
      (exit 1 (parse-opt-errors->str errors)))
    (when (nil? config-file)
      (exit 2 "Please supply a configuration file via -c option"))
    (let [cfg (read-config-file config-file)]
      (when-let [errors (s/check config-schema cfg)]
        (exit 3 (str "Please fix the configuration file: " errors)))
      (init! cfg)
      (uber-siphon cfg))))


(comment
  (def test-config
    {:kafka-source
     {:zookeeper-connect  "docker:49153"
      :connect-retry      5000
      :group-id           "qanal"
      :auto-offset-reset  :earliest     ; Can only be earliest or latest
      :fetch-size         (* 10 1024 1024)}
      :topics {"test1" :all
              "test3" [0 2]}
     :elasticsearch-target {:end-point "http://localhost:9200"}})

  (uber-siphon test-config)

  )
