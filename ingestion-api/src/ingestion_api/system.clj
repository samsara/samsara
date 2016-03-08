(ns ingestion-api.system
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [ingestion-api.backend.backend :as backend]
            [ingestion-api.input.http :as http]
            [ingestion-api.input.http-admin :as admin]
            [ingestion-api.components.mqtt-server :as mqtt]
            [ingestion-api.core.processors :as ps]
            [ingestion-api.backend.backend-protocol :refer [send]]
            [samsara.trackit :refer [track-time track-distribution]]))



(defn ingestion-api-system
  [config]
  (component/system-map
   :backend      (backend/new-backend config)
   :admin-server (admin/new-admin-server config)
   :http-server  (component/using
                  (http/new-http-server config) {:backend :backend})
   :mqtt-server  (component/using
                  (mqtt/new-mqtt-server config) {:backend :backend})))
