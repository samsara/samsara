(ns ingestion-api.core.processors
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


(defn process-events [events-seq & {:keys [posting-timestamp]}]
  "Takes a sequence of events (and optional values) processes them and sends
   on to the backend,
   returns a map with the :status (:success or :error)
   and optionally :error-msg
   e.g
   {:status :success
    :processed-events ({:timestamp 1456485005659
                        :sourceId \"a-user\"
                        :eventName \"test-event\"
                        :receivedAt 1456485697908
                        :publishedAt 1456485604501}
                       {:timestamp 1456485005659
                        :sourceId \"a-user\"
                        :eventName \"test-event\"
                        :receivedAt 1456485697908
                        :publishedAt 1456485604501})}
   {:status :error
    :error-msgs (\"OK\" {:timestamp (not (integer? \"not a timestamp\"))})}
  "
  (if-let [errors (is-invalid? events-seq)]
    {:status :error :error-msgs (map #(if % % "OK") errors)}
    (do
      (as-> events-seq $$
        (inject-receivedAt (System/currentTimeMillis) $$)
        (inject-publishedAt posting-timestamp $$)
        {:status :success :processed-events $$}))))


(comment

  (def a-time (java.util.Date.))
  (def valid-event {:timestamp (.getTime a-time)
                    :sourceId "a-user"
                    :eventName "test-event"
                    :test-prop "test-value"})
  (def invalid-event {:timestamp "timestamp?"
                      :sourceId "a-user"
                      :eventName "test-event"
                      :test-prop "test-value"})
  (def valid-seq  (repeat 3 valid-event))
  (def invalid-seq (take 3 (cycle [valid-event invalid-event])))

  (process-events valid-seq)
  (process-events valid-seq :posting-timestamp (.getTime #inst "2016-02-26T11:20:04.501-00:00"))
  (process-events invalid-seq)

  )
