(ns ingestion-api.status)

(def status-online? (atom true))

(defn change-status! [status]
  (case status
    "online"  (swap! status-online? (constantly true))
    "offline" (swap! status-online? (constantly false))
    :error))

(defn is-online? []
  @status-online?)
