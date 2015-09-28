(ns mqtt.domain.connack
  (:require [gloss.core :as gloss]
            [gloss.core.codecs :as codec]
            [gloss.io :as glossio]
            [mqtt.domain.util :refer :all]))
;;CONNACK

;; Codec for the CONNACK message.
(gloss/defcodec connack-codec
  (gloss/ordered-map
   :control-packet-type :byte
   :remaining-length    :byte
   :session-present     :byte
   :connection-status   :byte))

(def connack-connection-status {:accepted 0x00
                                :unacceptable-protocol-version 0x01
                                :identifier-rejected 0x02
                                :server-unavailable 0x03
                                :bad-username-password 0x04
                                :not-authorized 0x05})

(defn mqtt-connack->bytes
  "Convert MQTT connack message to bytes."
  [{:keys [session-present status]}]
  (.array (glossio/contiguous
           (glossio/encode connack-codec
                           {:control-packet-type (bit-shift-left 2 4)
                            :remaining-length    2
                            :session-present     (if session-present 0x01 0x00)
                            :connection-status   (get connack-connection-status status)}))))

