(ns ingestion-api.mqtt.handler
  (:use org.httpkit.server)
  (:require [ingestion-api.mqtt.service.connect :refer [connect]]
            [ingestion-api.mqtt.service.publish :refer [publish]]
            [ingestion-api.mqtt.service.ping    :refer [pingreq]]
            [ingestion-api.mqtt.domain.util     :refer [get-cp-type get-mqtt-message-type to-byte-array]]))

;;MQTT Message handler.
;;The plan is to not support too much of the protocol, so
;;I will put in all handling code in one file.

(defmacro defhandlers
  "Macro to define MQTT handlers by message type.
   The handler should be a function of arity 1.
   This macro does not validate the signature of
   the handler function at the moment."
  [name doc-string & key-values]
  (let [handlers (->> key-values (apply hash-map))]
    `(defn ~name [data#]
       (let [cp-type#       (-> data# first (get-cp-type))
             mqtt-msg-type# (get-mqtt-message-type cp-type#)
             handler#       (mqtt-msg-type# ~handlers)]
         (handler# data#)))))


(defhandlers mqtt-handler
  "MQTT handlers"
  :connect  connect
  :publish  publish
  :pingreq  pingreq)
