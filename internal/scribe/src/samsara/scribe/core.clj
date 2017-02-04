(ns samsara.scribe.core
  (:refer-clojure :exclude [read]))

(defmulti scribe :type)


(defprotocol Scribe

  (write [this data]
    "Returns a binary representation of the Clojure data")

  (read [this bytez]
    "Returns the Clojure data structure which was serialized with `write`."))
