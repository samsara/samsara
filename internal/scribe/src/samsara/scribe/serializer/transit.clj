(ns samsara.scribe.serializer.transit
  (:refer-clojure :exclude [read])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:require [cognitect.transit :as transit]
            [samsara.scribe.protocol :refer :all]))


(def ^:const DEFAULT-CONFIG
  {:size 1024, :format :json})


(defrecord TransitScribe [config]

  Scribe

  (write [_ data]
    (let [out (ByteArrayOutputStream. (:size config))
          writer (transit/writer out (:format config))]
      (transit/write writer data)
      (.toByteArray out)))

  (read [_ bytez]
    (let [in (ByteArrayInputStream. bytez)
          reader (transit/reader in (:format config))]
      (transit/read reader))))


(defn make-transit-scribe
  ([]
    (make-transit-scribe nil))
  ([config]
    (TransitScribe. (merge DEFAULT-CONFIG config))))
