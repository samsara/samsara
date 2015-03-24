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
(ns qanal.metrics
  (:require [qanal.utils :refer [execute-if-elapsed]]
            [taoensso.timbre :as log]
            [metrics.core  :refer [default-registry metric-name]]
            [metrics.gauges :refer [gauge-fn]]
            [metrics.counters :refer [counter inc!]]
            [metrics.timers :refer [timer]])
  (:import [java.util.concurrent TimeUnit]
           [com.codahale.metrics.riemann RiemannReporter Riemann]
           [com.aphyr.riemann.client RiemannClient]
           ))


(def ^:private default-riemann-port 5555)
(def ^:private default-poll-rate 1)

(defn- generate-metric-name [name]
  ["samsara" "qanal" name])

(defn connect-to-riemann [riemann-host]
  (when riemann-host
    (try
      (let [riemann-client (Riemann. (RiemannClient/tcp riemann-host default-riemann-port))
            builder (RiemannReporter/forRegistry default-registry)
            riemann-reporter (.build builder riemann-client)]
        (log/info "Connecting to Riemann Host " riemann-host)
        (.connect riemann-client)
        (log/info "Starting Riemann Reporter")
        (.start riemann-reporter default-poll-rate TimeUnit/SECONDS)
        (log/info "Connected to Riemann Host and Riemann Reporter started")
        riemann-reporter)
      (catch Exception e
        (log/error "An exception occured whilst connecting to Riemann Host and starting Reporter ->"
                   e)))))

(defn record-qanal-counters [{:keys [topic partition-id kafka-msgs-count bulked-docs]}]
  (let [base-name (str topic "." partition-id)
        kafka-msgs-metric (counter (generate-metric-name (str base-name ".msgs")))
        bulked-docs-metric (counter (generate-metric-name (str base-name ".bulkedDocs")))]
    (inc! kafka-msgs-metric kafka-msgs-count)
    (inc! bulked-docs-metric bulked-docs)))

(defn record-qanal-gauges [topic partition-id {:keys [msg-rate-fn bulking-rate-fn bulked-time-fn backlog-fn]}]
  (let [base-name (str topic "." partition-id)]
    (gauge-fn (generate-metric-name (str base-name ".msgRate")) msg-rate-fn)
    (gauge-fn (generate-metric-name (str base-name ".bulkingRate")) bulking-rate-fn)
    (gauge-fn (generate-metric-name (str base-name ".bulkedTime")) bulked-time-fn)
    (gauge-fn (generate-metric-name (str base-name ".backlog")) backlog-fn)
    ))


(def ^:private last-time-logged (atom 0))

(defn sensibly-log-stats [{:keys [topic partition-id kafka-msgs-count kafka-msgs-time bulked-docs bulked-time]}]
  (let [msg-rate (double (/ kafka-msgs-count (if (> kafka-msgs-time 0) kafka-msgs-time 1)))
        ze-fn (fn [] (log/info "Topic->" topic " Partition-id->" partition-id " Msgs->" kafka-msgs-count
                               " Msg Rate->" msg-rate " Bulked Docs->" bulked-docs " Bulked-time->" bulked-time "ms"))
        result (execute-if-elapsed ze-fn @last-time-logged 60000)]
    (when (:executed result)
      (reset! last-time-logged (System/currentTimeMillis)))))

(comment
  (def reporter (connect-to-riemann "127.0.0.1"))
  (.stop reporter)

  (record-qanal-counters {:topic "test-topic"
                       :partition-id 77
                       :kafka-msgs-count 4
                          :bulked-docs 5})

  (record-qanal-gauges "test-topic" 77 {:msg-rate-fn      (fn [] (* 10 (rand 3)))
                                        :bulking-rate-fn  (fn [] (* 10 (rand 3)))
                                        :bulked-time-fn   (fn [] (rand-int 300))
                                        :backlog-fn       (fn [] (rand-int 100))})

  )