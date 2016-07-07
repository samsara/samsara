(ns samsara-core.core
  (:require [moebius.core :refer :all])
  (:require [moebius.kv :as kv])
  (:require [samsara-core.time :refer [is-timestamp-reliable]])
  (:require [digest :refer [sha-256]]))


(defenrich inject-kibana-timestamp
  #_"Adds the `ts` attribute suitable for Kibana4"
  [{:keys [timestamp receivedAt publishedAt] :as event}]
  (-> event
      (inject-as :ts (java.util.Date. timestamp))
      (inject-as :receivedAtTs (and receivedAt (java.util.Date. receivedAt)))
      (inject-as :publishedAtTs (and publishedAt (java.util.Date. publishedAt)))))



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
   ;;is-timestamp-reliable
   inject-id
   ;;event-sequencer
   ))



(defn make-samsara-processor [config]
  (moebius (make-samsara-pipeline config)))
