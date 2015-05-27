(ns samsara-core.samza
  (:require [samsara.utils :refer [to-json from-json]]))

;; runtime pipeline initialized by init-pipeline!
(def internal-pipeline nil)


(defn process-kafka-event [event]
  (->> event
       from-json
       vector
       internal-pipeline
       (map (juxt :sourceId to-json))))


(defn pipeline
  "Takes an event in json format and returns a list of tuples
  with the partition key and the event in json format
  f(x) -> List<[part-key, json-event]>"
  [^String event]
  (process-kafka-event event))
