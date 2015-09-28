(ns ingestion-api.mqtt.domain.util
  (:require [clojure.math.numeric-tower :refer [abs]]))

;; Utility methods for domain namespaces

(defn get-cp-type
  "Returns Control Packet Type"
  [byte]
  (let [b (if (bit-test byte 7) (+ 128 (abs byte)) byte)]
    (bit-shift-right b 4)))


(defn to-byte-array [s]
  "Converts a sequence a byte-array"
  (byte-array (map byte s)))

(defn gloss-int-type
  "Returns the right int type key for the supplied bytecount"
  [byte-count]
  (case byte-count
    1 :byte
    2 :int16
    3 :int32
    4 :int64
    nil))

(def mqtt-message-types {1  :connect
                         2  :connack
                         3  :publish
                         4  :puback
                         ;;5 :pubrec
                         ;;6 :pubrel
                         ;;7 :pubcomp
                         ;;8 :subscribe
                         ;;9  suback
                         ;;10 :unsubscribe
                         ;;11 :unsuback
                         12 :pingreq
                         13 :pingresp
                         14 :disconnect})

(defn get-mqtt-message-type
  "Returns a human readable MQTT message type"
  [control-packet-type]
  (get mqtt-message-types control-packet-type :not-supported))
