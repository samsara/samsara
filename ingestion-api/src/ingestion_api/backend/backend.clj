(ns ingestion-api.backend.backend
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [ingestion-api.backend
             [backend-console :refer [make-console-backend]]
             [backend-kafka :refer [make-kafka-backend]]
             [backend-protocol :as protocol]]
            [samsara.trackit :refer [track-time]]
            [taoensso.timbre :as log]))

;; Backend component
(defn- init-backend
  "Create the specified backend where to send the events"
  [{:keys [type] :as cfg}]
  (log/info "Creating backend type: " type ", with config:" cfg)
  (case type
    :console (make-console-backend cfg)
    :kafka   (make-kafka-backend   cfg)
    (throw (RuntimeException. "Illegal backed type:" type))))



(defrecord Backend [config backend]
  ;;Implement Lifecycle
  component/Lifecycle

  (start [this]
    (if backend
      this
      (assoc this :backend
             (init-backend config))))

  (stop [this]
    (dissoc this :backend)))



(defn send
  [backend events]
  (track-time "ingestion.batch.backend-send"
              (protocol/send (:backend backend) events)))



(defn new-backend
  "Initialize the backend component."
  [config]
  (map->Backend {:config  (:backend config)}))
