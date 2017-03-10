(ns samsara.scribe.serializer.nippy
  (:refer-clojure :exclude [read])
  (:require [samsara.scribe.protocol :refer :all]
            [taoensso.nippy :as nippy]))


(def ^:const DEFAULT-CONFIG {})


(defrecord NippyScribe [config]

  Scribe

  (write [_ data]
    (nippy/freeze data config))

  (read [_ bytez]
    (nippy/thaw bytez config)))


(defn make-nippy-scribe
  ([]
    (make-nippy-scribe nil))
  ([config]
    (NippyScribe. (merge DEFAULT-CONFIG config))))
