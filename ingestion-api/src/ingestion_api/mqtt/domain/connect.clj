(ns mqtt.domain.connect
  (:require [gloss.core :as gloss]
            [gloss.core.codecs :as codec]
            [gloss.io :as glossio]
            [mqtt.domain.util :refer :all]))
;; MQTT Connect

;; This is the order in which the connect
;; flags are specified in CONNECT msg.
(def mqtt-connect-flags
  [:username
   :password
   :will-retain
   :will-qos
   :will-flag
   :clean-session
   :reserved])

(defn- get-remaining-length
  "Returns remaining length and pos of variable header"
  [data]
  ;;Determine if the top bit is set. If yes, there is more.
  ;;TODO Err if multipler > 128*128*128
  (let [bytes (vec data)]
    (loop [index 1
           multiplier 1
           prev-value 0]
      (let [eb (get bytes index)
            value (+ prev-value (* multiplier (bit-and eb 127)))]
        (if (= 0 (bit-and eb 128))
          {:remaining-length value :bytes-used index}
          (recur (inc index)
                 (* multiplier 128)
                 value))))))


(defn connect-codec
  "Returns a codec to parse CONNECT message given the remaining length.
  It is not ideal to compile a new frame for every request, but gloss
  doesnt support it at the moment. TODO: Can we write a custom header?"
  [remaining-len]
  (gloss/compile-frame
   (gloss/compile-frame
    (gloss/ordered-map
     :control-packet-type :byte
     :remaining-length    (gloss-int-type remaining-len)
     :protocol-name       (gloss/string :utf-8
                                        :prefix (gloss/prefix :int16))
     :protocol-level      :byte
     :connect-flags       (gloss/bit-seq 1 1 1 2 1 1 1)
     :keep-alive          :int16
     :payload             (gloss/repeated :byte :prefix :none)))))



;; Frame for the rest of the payload.
;; Payload is a bunch of payload items specified by the connect flags
;; [See: payload-item-fr] the actual payload.
(def payload-response-frame (gloss/repeated
                             (gloss/string
                              :utf-8
                              :prefix (gloss/prefix :int16))
                             :prefix :none))

;; Frame for a single payload item. Each item in the payload is a string
;; prefixed with the size (int16).
(def payload-item-fr
  (gloss/string :utf-8 :prefix (gloss/prefix :int16)))


;; The payload items appear in the payload in the following order:
;; :client-id :will-topic :will-message :username :password :response
;; This is followed by the actual payload.
(defn- payload-frame
  "Frame for the overall payload. This is a bunch of payload items
   specified by the connect flags and the payload itself."
  [connect-flags]
  (remove nil?  (-> [:client-id payload-item-fr]
                    (conj (if (:will-flag connect-flags)
                            [:will-topic payload-item-fr
                             :will-message payload-item-fr] nil))
                    (conj (if (:username connect-flags)
                            [:username payload-item-fr] nil))
                    (conj (if (:password connect-flags)
                            [:password payload-item-fr] nil))
                    (conj [:response payload-response-frame])
                    (flatten))))

(defn- payload-codec
  "Codec for the payload."
  [payload-frame]
  (->> payload-frame
       (apply gloss/ordered-map)
       (gloss/compile-frame)))


(defn bytes->mqtt-connect
  "Converts byte array to a human readable mqtt connect message"
  [data]
  (let [{:keys
         [bytes-used]}  (get-remaining-length data)
         codec           (connect-codec bytes-used)
         parsed          (glossio/decode codec (to-byte-array data))
         conn-flags      (vec (interleave mqtt-connect-flags
                                          (:connect-flags parsed)))
         payload-frame   (payload-frame  (apply hash-map conn-flags))
         payload         (glossio/decode
                          (payload-codec payload-frame)
                          (to-byte-array (:payload parsed)))]
    (-> parsed
        (get :control-packet-type)
        (get-cp-type)
        (get-mqtt-message-type)
        ((fn [i] {:message-type i}))
        (assoc :protocol-name  (:protocol-name parsed))
        (assoc :protocol-level (:protocol-level parsed))
        (assoc :connect-flags  conn-flags)
        (conj payload))))












