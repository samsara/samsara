(ns ingestion-api.events
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend :refer :all]))

;;
;; This is the backend where events are send to
;; and it is initialized by core/init! with
;; one of the implementation of EventsQueueingBackend protocol
;;
(def ^:dynamic *backend* (atom nil))


(defn send!
  "Sends the events to the configured backend queuing system"
  [events]
  (send @*backend* events))
