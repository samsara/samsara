(ns samsara.scribe.serializer.fressian
  (:refer-clojure :exclude [read])
  (:require [samsara.scribe.protocol :refer :all]
            [clojure.data.fressian :as fress]))

(defrecord FressianScribe []

  Scribe

  (write [_ data]
    (fress/write data))

  (read [_ bytez]
    (fress/read bytez)))


(defn make-fressian-scribe
  []
  (FressianScribe.))
