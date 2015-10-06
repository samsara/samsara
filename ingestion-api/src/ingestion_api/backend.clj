(ns ingestion-api.backend
  (:refer-clojure :exclude [send])
  (:require [clojure.pprint :refer [pprint]]))

(defprotocol EventsQueueingBackend
  "Backend queueing system abstraction where events are sent to.
  The purpose of this protocol is to abstract the backend
  and enable a pluggable architecture where different backends
  can be used."

  (send [this events]
    "send the events to the backend queueing system"))


;;
;; This backend is for testing purposes, it just prints
;; the events to the stdout.
;;
(deftype ConsoleBackend [pretty?]
  EventsQueueingBackend

  (send [_ events]
    (println
     ;; to avoid multi-thread
     ;; data interleave
     (with-out-str
       (doseq [e events]
         (if pretty?
           (pprint e)
           (println e)))))))


(defn make-console-backend
  "Create a console backend"
  [{pretty? :pretty?}]
  (ConsoleBackend. pretty?))
