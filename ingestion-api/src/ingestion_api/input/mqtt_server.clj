(ns ingestion-api.input.mqtt-server
  (:refer-clojure :exclude [send])
  (:require [taoensso.timbre :as log])
  (:require [com.stuartsierra.component :as component]
            [samsara-mqtt.mqtt-server :as mqtt]
            [ingestion-api.core.processors :as ps]
            [ingestion-api.backend.backend :refer [send]]
            [samsara.utils :refer [from-json]]))



(defn mqtt-callback
  "Returns a callback that can be set to MQTT component"
  [backend]
  (fn [data]
    (doseq [m (:payload data)]
      (->> (from-json m)
           ps/process-events
           ;; TODO: handle error
           :processed-events
           (send backend)))))



(defrecord MqttServer [port enabled backend mqtt-server]
  component/Lifecycle

  (start [component]
    (when enabled
      (if mqtt-server
        component
        (->>
         (mqtt-callback backend)
         (mqtt/start port)
         (assoc component :mqtt-server)))))

  (stop [component]
    (if mqtt-server
      (do (->> component :mqtt-server mqtt/stop)
          (dissoc component :mqtt-server))
      component)))



(defn new-mqtt-server
  [config]
  (map->MqttServer (:mqtt config)))
