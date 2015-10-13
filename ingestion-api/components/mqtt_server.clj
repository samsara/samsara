(ns ingestion-api.mqtt-server
  (:require [com.stuartsierra.component :as component]
            [aleph.tcp :as tcp]
            [ingestion-api.mqtt.tcp :refer [mqtt-handler]]))

(defrecord MqttServer [port enabled]
  component/Lifecycle

  (start [component]
    (if enabled
      (->>
       {:port port}
       (tcp/start-server mqtt-handler)
       (assoc component :tcp-server))))

  (stop [component]
    (some->>
     component
     (:tcp-server)
     (.close)
     (assoc component :tcp-server))))

(defn new-mqtt-server [config]
  (map->MqttServer (:mqtt config)))


