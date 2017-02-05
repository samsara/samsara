(ns samsara.scribe.serializer.json
  (:refer-clojure :exclude [read])
  (:require [samsara.scribe.protocol :refer :all]
            [cheshire.core :as json]))


(def ^:const DEFAULT-CONFIG
  {:date-format "yyyy-MM-dd'T'HH:mm:ss.SSSX"})



(defrecord JsonScribe [config]

  Scribe

  (write [_ data]
    (let [^String payload (json/generate-string data config)]
      (.getBytes payload "UTF-8")))


  (read [_ bytez]
    (let [^String payload (String. ^bytes bytez "UTF-8")]
      (json/parse-string payload true))))



(defn make-json-scribe
  ([]
   (make-json-scribe nil))
  ([config]
   (JsonScribe. (merge DEFAULT-CONFIG config))))
