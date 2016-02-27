(ns ingestion-api.backend.backend
  (:refer-clojure :exclude [send])
  (:require [taoensso.timbre :as log]
            [ingestion-api.backend.backend-protocol :refer [send]]
            [ingestion-api.backend.backend-kafka
             :refer [make-kafka-backend make-kafka-backend-for-docker]]
            [ingestion-api.backend.backend-console :refer [make-console-backend]]
            [com.stuartsierra.component :as component]))

;; Backend component
(defn- init-backend
  "Create the specified backend where to send the events"
  [{:keys [type] :as cfg}]
  (log/info "Creating backend type: " type ", with config:" cfg)
  (case type
    :console (make-console-backend cfg)
    :kafka   (make-kafka-backend   cfg)
    :kafka-docker   (make-kafka-backend-for-docker cfg)
    (throw (RuntimeException. "Illegal backed type:" type))))


(defrecord Backend [config backend]
  ;;Implement Lifecycle
  component/Lifecycle

  (start [this]
    (if backend this
        (assoc this :backend
               (init-backend config))))

  (stop [this]
    (dissoc this :backend)))

(defn new-backend
  "Initialize the backend component."
  [config]
  (map->Backend {:config  (:backend config)}))
