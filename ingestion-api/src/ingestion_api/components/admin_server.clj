(ns ingestion-api.components.admin-server
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [aleph.http :refer [start-server]]
            [ingestion-api.input.http :refer [admin-app]]))

(def default-values {:port 9010})

(defrecord AdminServer [server-instance port]
  component/Lifecycle

  (start [component]
    (log/info "Samsara Admin listening on port:" port)
    (if server-instance component
        (->>  (start-server admin-app {:port port})
             (assoc component :server-instance))))

  (stop [component]
    (if server-instance
      (update component :server-instance #(.close %))
      component)))

(defn new-admin-server
  [config]
  (let [server-config (merge default-values (:admin-server config))]
    (map->AdminServer server-config)))
