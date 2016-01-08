(ns ingestion-api.mqtt.service.connect
  (:require [ingestion-api.mqtt.domain.connect :refer [bytes->mqtt-connect]]
            [ingestion-api.mqtt.domain.connack :refer [mqtt-connack->bytes]]
            [schema.core :as s]))

;;; The Service will take an MQTT message as map
;;; Return an MQTT message as a map or
;;; an Exception.

(def connect-schema
  "Schema for MQTT CONNECT message."
  {:message-type   (s/eq :connect)
   :client-id      s/Str
   :protocol-name  (s/enum "MQIsdp" "MQTT")
   ;; MUST be 4 acc to spec, but clients send 3 sometimes
   ;; Muting this validation for now.
   ;; :protocol-level (s/eq 3)
   s/Keyword s/Any})


(defn parse-request
  "Parse and validate byte-array to mqtt-connect.
   Returns a map containing :request and :error."
  [req-bytes]
  (let [req (bytes->mqtt-connect req-bytes)
        err (s/check connect-schema req)]
    {:request req :error err}))


(defn connect
  "Handles MQTT connect."
  [req-bytes]
  (let [{:keys [request error]} (parse-request req-bytes)]
    (when error
      (throw (ex-info "Invalid message received:" error)))
    ;; request is fine. return connack
    (mqtt-connack->bytes {:session-present true :status :accepted})))
