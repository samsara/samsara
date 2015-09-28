(ns mqtt.service.ping
  (:require [mqtt.domain.ping :refer [mqtt-pingresp->bytes]]))

(defn pingreq
  "Handles PINGREQ"
  [data]
  (mqtt-pingresp->bytes true))
