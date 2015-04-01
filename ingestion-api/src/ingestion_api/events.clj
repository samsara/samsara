(ns ingestion-api.events
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend :refer :all])
  (:require [schema.core :as s]))

;;
;; This is the backend where events are send to
;; and it is initialized by core/init! with
;; one of the implementation of EventsQueueingBackend protocol
;;
(def ^:dynamic *backend* (atom nil))

;;
;; if provided *transform-fn* is applied to every event
;; before to be sent to the specified backend
;;
(def !transform-fn! nil)


(defn apply-transformation [events]
  (let [trf (or !transform-fn! identity)]
    (map trf events)))


(defn send!
  "Sends the events to the configured backend queuing system"
  [events]
  (send @*backend* events))


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
  (s/check events-schema events))


(defn inject-receivedAt
  "it injects the timestamp of when the events were received
  by the servers."
  [receivedAt events]
  (map #(update-in % [:receivedAt] (fn [ov] (or ov receivedAt))) events))
