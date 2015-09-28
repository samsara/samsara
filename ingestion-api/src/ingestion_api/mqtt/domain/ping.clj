(ns mqtt.domain.ping
  (:require [mqtt.domain.util :refer [to-byte-array]]))

;; PINGREQ is a simple message with only CP set.
;; We shouldnt have to decode at all. Just react
;; by sending a PINGRES

(def pingresp (to-byte-array '(-80 0)))

(defn mqtt-pingresp->bytes
  "Returns a PINGRESP to keep-alive."
  [alive?]
  (if alive? pingresp))

