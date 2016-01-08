(ns ingestion-api.mqtt.service.ping
  (:require [ingestion-api.mqtt.domain.ping :refer [mqtt-pingresp->bytes]]))

(defn pingreq
  "Handles PINGREQ"
  [data]
  (mqtt-pingresp->bytes true))
