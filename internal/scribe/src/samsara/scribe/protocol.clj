(ns samsara.scribe.protocol
  (:refer-clojure :exclude [read]))


(defprotocol Scribe

  (write [this data]
    "Returns a binary representation of the Clojure data.
     It returns a byte[]")

  (read [this ^bytes bytez]
    "Returns the Clojure data structure which was serialized with `write`."))
