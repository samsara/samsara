(ns ingestion-api.mqtt.service.publish
  (:require [ingestion-api.mqtt.domain.publish :refer [bytes->mqtt-publish]]
            [schema.core :as s]
            [cheshire.core :as json]))


(def Publish
  "Schema for MQTT PUBLISH message."
  {:message-type (s/eq :publish)
   :qos          (s/eq 0)
   :topic        (s/eq "topic/events")
   s/Any s/Any})

(defn parse-request
  "Parse and validate byte-array to mqtt-publish.
   Returns a map containing :request and :error"
  [req-bytes]
  (let [req (bytes->mqtt-publish req-bytes)
        err (s/check Publish req)]
    {:request req :error err}))

(defn publish
  "Handles MQTT Publish. We only handle QOS-0, 
   so there is no response required. Just return nil."
  [req-bytes]
  (let [{:keys [request error]} (parse-request req-bytes)]
    (if (not (nil? error))
      (assert false (str "Invalid publish message received:" error)))
    (println "Received @ " (:topic request)
             " Message: " (json/parse-string (:payload request)))
    nil))

