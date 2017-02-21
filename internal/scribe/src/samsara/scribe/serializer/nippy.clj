(ns samsara.scribe.serializer.nippy
  (:refer-clojure :exclude [read])
  (:require [samsara.scribe.protocol :refer :all]
            [taoensso.nippy :as nippy]))


(defrecord NippyScribe [config]

  Scribe

  (write [_ data]
    (nippy/freeze data))

  (read [_ bytez]
    (nippy/thaw bytez)))


(defn make-nippy-scribe
  ([]
    (NippyScribe. nil)))
