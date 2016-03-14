(ns scratch
  (:require [cheshire.core :refer :all]
            [clojurewerkz.machine-head.client :as mh]))


(def events [{:sourceId "5340-dfd0350"
              :eventName "session.created"
              :timestamp (System/currentTimeMillis)
              :user      "svittal@gmail.com"}])

(defn publish
  [events]
  (let [id   (mh/generate-id)
        conn (mh/connect "tcp://localhost:10010" id)]
    (mh/publish conn "topic/events" events 0)))

(publish (generate-string events))
