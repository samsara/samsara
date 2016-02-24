(ns ingestion-api.events
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend.api :refer :all])
  (:require [reloaded.repl :refer [system]])
  (:require [schema.core :as s])
  (:require [samsara.trackit :refer [track-time]]))


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


(defn send!
  [events posting-ts backend]
  (track-time "ingestion.events.backend-send"
              (->> events
                   (inject-receivedAt (System/currentTimeMillis))
                   (inject-publishedAt posting-ts)
                   (send backend))))
