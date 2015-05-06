(ns moebius.samsara
  (:require [moebius.core :refer :all]))



(defenrich inject-kibana-timestamp
  #_"Adds the `ts` attribute suitable for Kibana4"
  [{:keys [timestamp] :as event}]
  (inject-as event :ts (java.util.Date. timestamp)))



(defenrich is-timestamp-reliable
  #_"It verifies if the client timestamp seems reliable,
   and if it is it will inject and attribute `timeReliable`
   true/false."
  [{:keys [timestamp receivedAt publishedAt] :as event}]
  (when-event-is
   event (and timestamp receivedAt publishedAt)
   (assoc event :timeReliable
          (cond
           ;; is the timestamp coming from
           ;; the future
           (> timestamp publishedAt)  false
           (> publishedAt receivedAt) false
           ;; is received - published within
           ;; a reasonable time (10s) otherwise is bogus
           (not (>= 10000 (- receivedAt publishedAt) 0)) false
           ;; is the event generated within a
           ;; reasonable time? 10d otherwise is bogus
           (not (>= (* 10 24 60 60 1000) (- publishedAt timestamp) 0)) false
           :else true))))



(def samsara-pipeline
  (pipeline
   inject-kibana-timestamp
   is-timestamp-reliable))
