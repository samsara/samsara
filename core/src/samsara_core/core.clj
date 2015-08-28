(ns samsara-core.core
  (:require [moebius.core :refer :all])
  (:require [moebius.kv :as kv])
  (:require [digest :refer [sha-256]]))


(defenrich inject-kibana-timestamp
  #_"Adds the `ts` attribute suitable for Kibana4"
  [{:keys [timestamp] :as event}]
  (inject-as event :ts (java.util.Date. timestamp)))



(defenrich is-timestamp-reliable
  #_"It verifies if the client timestamp seems reliable,
   and if it is it will inject and attribute `timeReliable`
   true/false."
  [{:keys [timestamp receivedAt publishedAt] :as event}]
  (when (and timestamp receivedAt publishedAt)
        (assoc event :timeReliable
               (cond
                ;; is the timestamp coming from
                ;; the future
                (> timestamp publishedAt)  false
                (> publishedAt receivedAt) false
                ;; is received - published within
                ;; a reasonable time (10s) otherwise we consider ti bogus
                (not (>= 10000 (- receivedAt publishedAt) 0)) false
                ;; is the event generated within a
                ;; reasonable time? 10d otherwise is bogus
                (not (>= (* 10 24 60 60 1000) (- publishedAt timestamp) 0)) false
                :else true))))



(defenrich inject-id
  #_"It inject a consistent and repeatable ID is not already provided"
  [{:keys [timestamp eventName sourceId id] :as event}]
  (when-not id
    (inject-as event :id (sha-256 (str timestamp "/" eventName "/" sourceId)))))



(defenrich event-sequencer
  #_"It adds a 'seqn' attribute to the event with a monotonic incremental counter
    which helps to generate a sequence of the order the events have been processed."
  [state {:keys [sourceId] :as event}]
  (let [seqn   (inc (or (kv/get state sourceId :count) 0))
        state' (kv/set state sourceId :count seqn)]
    [state' (assoc event :seqn seqn)]))



(defn make-samsara-pipeline [config]
  (pipeline
   inject-kibana-timestamp
   is-timestamp-reliable
   inject-id
   event-sequencer))



(defn make-samsara-processor [config]
  (moebius (make-samsara-pipeline config)))
