(ns qanal.metrics
  (:require [taoensso.timbre :as log]
            [metrics.core  :refer [default-registry]]
            [metrics.counters :refer [counter inc!]]
            [metrics.gauges :refer [gauge-fn]])
  (:import [java.util.concurrent TimeUnit]
           [com.codahale.metrics.riemann RiemannReporter Riemann]
           [com.aphyr.riemann.client RiemannClient]))

(def ^:private default-riemann-port 5555)
(def ^:private default-poll-rate 10)

(defn- generate-metric-name [name]
  ["samsara" "qanal" name])

(defn connect-to-riemann [riemann-host]
  (when riemann-host
    (try
      (let [riemann-client (Riemann. (RiemannClient/tcp riemann-host default-riemann-port))
            builder (RiemannReporter/forRegistry default-registry)
            builder (doto builder
                      (.prefixedWith "qanal"))
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

(defn record-qanal-stats [{:keys [topic partition-id received-msgs bulked-docs bulked-time backlog]}]
  (let [base-name (str topic "." partition-id)
        received-msgs-metric (counter (generate-metric-name (str base-name ".receivedMsgs")))
        bulked-docs-metric (counter (generate-metric-name (str base-name ".bulkedMsgs")))
        bulked-time-metric (counter (generate-metric-name (str base-name ".bulkedTime")))]
    (inc! received-msgs-metric received-msgs)
    (inc! bulked-docs-metric bulked-docs)
    (inc! bulked-time-metric bulked-time)
    (gauge-fn (generate-metric-name (str base-name ".backlog")) (fn [] backlog))))

(comment
  (def reporter (connect-to-riemann "127.0.0.1"))
  (.stop reporter)

  (record-qanal-stats {:topic "test-topic"
                       :partition-id "99"
                       :received-msgs 4
                       :bulked-docs 4
                       :bulked-time 2
                       :backlog 0}))