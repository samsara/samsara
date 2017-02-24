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
    (let [out (ByteArrayOutputStream. (config :size))
          writer (transit/writer out (config :format))]
      (do (transit/write writer data) out)))

  (read [_ bytez]
    (let [in (ByteArrayInputStream. (.toByteArray bytez))
          reader (transit/reader in (config :format))]
      (transit/read reader))))


(defn make-transit-scribe
  ([]
    (make-transit-scribe nil))
  ([config]
    (TransitScribe. (merge DEFAULT-CONFIG config))))
