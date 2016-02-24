(ns ingestion-api.backend.backend
  (:refer-clojure :exclude [send])
  (:require [clojure.pprint :refer [pprint]]))

(defprotocol EventsQueueingBackend
  "Backend queueing system abstraction where events are sent to.
  The purpose of this protocol is to abstract the backend
  and enable a pluggable architecture where different backends
  can be used."

  (send [this events]
    "send the events to the backend queueing system"))
