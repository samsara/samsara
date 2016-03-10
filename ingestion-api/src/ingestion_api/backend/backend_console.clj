(ns ingestion-api.backend.backend-console
  (:refer-clojure :exclude [send])
  (:require [clojure.pprint :refer [pprint]]
            [ingestion-api.backend.backend-protocol :refer [send]])
  (:import ingestion_api.backend.backend_protocol.EventsQueueingBackend))

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
