(ns ingestion-api.components.mqtt-server
  (:require [com.stuartsierra.component :as component]
            [aleph.tcp :as tcp]
            [ingestion-api.mqtt.tcp :refer [mqtt-handler]]))

(defrecord MqttServer [port enabled server]
  component/Lifecycle

  (start [component]
    (if enabled
      (if server
        component
        (->>
         {:port port}
         (tcp/start-server mqtt-handler)
         (assoc component :server)))))

  (stop [component]
    (if server
      (->> component :server .close (assoc component :server))
      component)))

(defn new-mqtt-server
  [config]
  (map->MqttServer (:mqtt config)))


