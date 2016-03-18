(ns ingestion-api.input.mqtt-server
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [ingestion-api.backend.backend :refer [send]]
            [ingestion-api.core.processors :as ps]
            [samsara
             [trackit :refer [track-distribution track-rate]]
             [utils :refer [from-json]]]
            [samsara-mqtt.mqtt-server :as mqtt]
            [taoensso.timbre :as log]))


(defn- send-to-backend
  [backend events]
  (track-rate "ingestion.mqtt.requests")
  (track-rate "ingestion.mqtt.events" (count events))
  (track-distribution "ingestion.mqtt.batch.size" (count events))
  (send backend events))



(defn- log-errors [events error-msg]
  (track-rate "ingestion.mqtt.requests-error")
  (log/warn "Received invalid events in the mqtt channel. Events: "
            (prn-str events)  "Reason:" (prn-str error-msg)))


(defn mqtt-callback
  "Returns a callback that can be set to MQTT component"
  [backend]
  (fn [data]
    (doseq [m (:payload data)]
      (let [events (from-json m)
            {:keys [status error-msgs processed-events]
             } (ps/process-events events)]
        (if (= :error status)
          (log-errors events error-msgs)
          (send-to-backend backend processed-events))))))



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
