(ns qanal.messagecodec
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]))


(defn decode-json [bytes]
  (with-open [rdr (clojure.java.io/reader bytes :encoding "UTF-8")]
    (try
      (json/parse-stream rdr keyword)
      (catch Exception e (log/warn "Unable to parse json due to this exception !! ->" e)))))

(defn validate-river-json [json-map]
  (let [{:keys [index type source]} json-map]
    (if (some nil? [index type source])
      (log/warn "The decoded json map doesn't represent a valid river-json. It should contain the following keys"
                " [index type source]. Decoded json map->" json-map)
      json-map)))

(defn decode-river-json [bytes]
  (if-let [json-map (decode-json bytes)]
    (validate-river-json json-map)))



(comment

  (def test-json (.getBytes "{\"index\":\"index-name\" , \"type\": \"my-type\", \"source\": \"my-source\"}"))
  (def json-map (decode-json test-json))
  (validate-river-json json-map)

  (decode-river-json test-json)
  )