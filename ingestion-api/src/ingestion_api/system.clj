(ns ingestion-api.system
  (:require [com.stuartsierra.component :as component]
            [ingestion-api.backend.backend :as backend]
            [ingestion-api.input
             [http :as http]
             [http-admin :as admin]
             [mqtt-server :as mqtt]]))


(defn ingestion-api-system
  [config]
  (component/system-map
   :backend      (backend/new-backend config)
   :admin-server (admin/new-admin-server config)
   :http-server  (component/using
                  (http/new-http-server config) {:backend :backend})
   :mqtt-server  (component/using
                  (mqtt/new-mqtt-server config) {:backend :backend})))
