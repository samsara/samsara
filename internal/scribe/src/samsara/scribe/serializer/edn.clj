(ns samsara.scribe.serializer.edn
  (:refer-clojure :exclude [read])
  (:require [samsara.scribe.protocol :refer :all]
            [clojure.edn :as edn]))

(def ^:const DEFAULT-CONFIG {})

(defrecord EdnScribe [config]

  Scribe

  (write [_ data]
    (pr-str data))

  (read [_ bytez]
    (edn/read-string config bytez)))


(defn make-edn-scribe
  ([]
    (make-edn-scribe nil))
  ([config]
    (EdnScribe. (merge DEFAULT-CONFIG config))))
