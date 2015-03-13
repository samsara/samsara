(ns ingestion-api.backend
  (:refer-clojure :exclude [send])
  (:require [clojure.pprint :refer [pprint]]))

(defprotocol EventsQueueingBackend
  "Backend queueing system abstraction where events are sent to."
  (send [this events]
    "send the events to the backend queueing system"))

(deftype ConsoleBackend [pretty?]
  EventsQueueingBackend

  (send [_ events]
    (doseq [e events]
      (if pretty?
        (pprint e)
        (println e)))))
