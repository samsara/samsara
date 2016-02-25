(ns ingestion-api.core.pipeline
  (:require [samsara.trackit :refer [track-time]]
            [schema.core :as s]))


(def single-event-schema
  "Schema validation for events"
  {
   (s/required-key :timestamp) s/Int
   (s/required-key :sourceId)  s/Str
   (s/required-key :eventName) s/Str
   s/Keyword s/Any
   })


(def events-schema
  "Schema for a batch of events"
  [ single-event-schema ])


(defn is-invalid?
  "check if the events sent are valid. If any of the events
  is invalid an error structure is returned. If all events
  are valid then nil is returned."
  [events]
  (track-time "ingestion.events.validation"
              (if-not (seq? events)
                ["Invalid format, content-type must be application/json"]
                (s/check events-schema events))))


(defn inject-receivedAt
  "it injects the timestamp of when the events were received
  by the servers."
  [receivedAt events]
  (map #(update-in % [:receivedAt] (fn [ov] (or ov receivedAt))) events))


(defn inject-publishedAt
  "it injects the timestamp of when the events were sent by the client
  to the servers."
  [published events]
  (if published
    (map #(update-in % [:publishedAt] (fn [ov] (or ov published))) events)
    events))


(defn process-events [events-seq & {:keys[posting-timestamp]}]
  "Takes a sequence of events (and optional values) validates and enriches them,
   returns a map with the :status (:success or :error)
   :events (if successful the enriched events otherwise the provided events)
   and optionally :error-msg
   e.g
   {:events
    ({:timestamp 1458414757925,
      :sourceId \"user-a\",
      :eventName \"test-event\",
      :event-class :test,
      :receivedAt 1456389378708}
     {:timestamp 1458414757925,
      :sourceId \"user-b\",
      :eventName \"test-event\",
      :event-class :test,
      :receivedAt 1456389378708}),
    :status :success}

   {:events
    ({:timestamp 1458414757925,
      :sourceId \"user-a\",
      :eventName \"test-event\",
      :event-class :test}
     {:timestamp \"not a timestamp\",
      :sourceId \"user-a\",
      :eventName \"test-event\",
      :event-class :test}),
    :error-msg
    (\"OK\" {:timestamp (not (integer? \"not a timestamp\"))}),
    :status :error}
  "
  (if-let [errors (is-invalid? events-seq)]
    (hash-map :status :error :error-msg (map #(if % % "OK") errors) :events events-seq)
    (as-> events-seq $$
      (inject-receivedAt (System/currentTimeMillis) $$)
      (inject-publishedAt posting-timestamp $$)
      (hash-map :status :success :events $$))))


(comment

  (def date #inst "2016-03-19T19:12:37.925-00:00")

  (def valid-event {
                    :timestamp (.getTime date)
                    :sourceId "user-a"
                    :eventName "test-event"
                    :event-class :test})

  (def invalid-event {
                     :timestamp "not a timestamp"
                     :sourceId "user-a"
                     :eventName "test-event"
                     :event-class :test} )

  (def valid-event-seq (take 3 (repeat valid-event )))

  (def invalid-event-seq (take 3 (cycle [valid-event invalid-event])))

  (process-events valid-event-seq)

  (process-events invalid-event-seq)

)
