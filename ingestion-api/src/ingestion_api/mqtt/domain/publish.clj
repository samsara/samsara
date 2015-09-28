(ns ingestion-api.mqtt.domain.publish
  (:require [gloss.core :as gloss]
            [gloss.core.codecs :as codec]
            [gloss.io :as glossio]
            [ingestion-api.mqtt.domain.util :refer :all]))
;; MQTT Publish

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

(defn- pkt-id-present?
  "Returns if Packet Identifier is present in the MQTT PUBLISH message.
   Packet Identifier is present if QOS is NOT 0"
  [bytes]
  (let [cp  (first bytes)
        qos (bit-and cp 6)]
    (not= qos 0)))

(defn base-fr
  "Returns base frame with given rem-len"
  [rem-len]
  [:control-packet-type (gloss/bit-seq 4 1 2 1)
   :remaining-length    (gloss-int-type rem-len)
   :topic               (gloss/string :utf-8
                                      :prefix (gloss/prefix :int16))])

(def payload-fr [:payload (gloss/repeated
                           (gloss/string :utf-8)
                           :prefix :none)])

(def pkt-id-fr [:packet-identifier :int16])

(defn publish-frame
  [rem-len pkt-id?]
  (remove nil?  (-> (base-fr rem-len)
                    (conj (if pkt-id? pkt-id-fr nil))
                    (conj payload-fr)
                    (flatten))))

(defn publish-codec
  "Returns a codec to parse PUBLISH message given the remaining length.
  It is not ideal to compile a new frame for every request, but gloss
  doesnt support it at the moment. TODO: Can we write a custom header?
  This codec is specific to QOS-0. It will blow up if QOS1/2 is requested."
  [rem-len pkt-id?]
  (->>
   (publish-frame rem-len pkt-id?)
   (apply gloss/ordered-map)
   (gloss/compile-frame)))


(defn bytes->mqtt-publish
  "Converts byte array to a human readable mqtt publish message"
  [data]
  (let [{:keys
         [bytes-used]}  (get-remaining-length data)
         pkt-id?        (pkt-id-present? data)
         codec          (publish-codec bytes-used pkt-id?)
         parsed         (glossio/decode codec (to-byte-array data))
         [cp-type dup? qos retain?]  (:control-packet-type parsed)]
    ;; NOTE - cp-type has already been right-shifted.
    ;; It means we dont need to call utils/get-cp-type.
    {:message-type (get-mqtt-message-type cp-type)
     :qos          qos
     :topic        (:topic parsed)
     :payload      (apply str (:payload parsed))}))









