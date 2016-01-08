(ns samsara-builtin-modules.session-boundaries
  (:require [clojure.string :as s])
  (:require [moebius.core :refer :all]
            [moebius.kv :as kv]))


(defn merge-session-events
  "Merges the session events and injects the duration in milliseconds"
  [proposed-name
   {ts1 :timestamp id1 :id :as event-start}
   {ts2 :timestamp id2 :id :as event-stop}]
  (-> (merge event-start event-stop)
      (dissoc :id)
      (inject-as :startTs ts1)            ;; add start & stop timestamps
      (inject-as :stopTs ts2)
      (inject-as :startEventId id1)      ;; add start & stop ids
      (inject-as :stopEventId  id2)
      (inject-as :timestamp ts1)         ;; use start timestamp
      (inject-as :duration (- ts2 ts1))  ;; add duration
      (inject-as :eventName proposed-name) ;; give it a name
      (inject-as :inferred true)))       ;; add the inferred flag



(def ^:dynamic *session-boundaries*
  ;; make sure the stop refers to the correct start
  ;; a.started -> b.started -> b.stopped [a.done] !!!!
  [{:start-gate (partial match-glob "**.started")
    :stop-gate  (partial match-glob "**.stopped")
    :start-gate-name #(s/replace % #"\.stopped$" ".started")
    :merge-with merge-session-events
    :name-fn    #(s/replace % #"\.started$" ".done")}])



(def ^:private -which-boundary
  (memoize
   (fn [session-boundaries]
     (->> session-boundaries
          (mapcat (fn [g] [{:spec g :type :start :boundary-check (:start-gate g)}
                          {:spec g :type :stop  :boundary-check (:stop-gate g)}]))))))



(defn- is-boundary-event? [{:keys [eventName]}]
  (some #(when ((:boundary-check %) eventName) %)
        (-which-boundary *session-boundaries*)))



(defn- mark-session-start
  "mark the session start for this specific event/sourceId.
   Return the new state which contains this info."
  [state {:keys [sourceId eventName] :as event} spec]
  (kv/set state sourceId [:session-boundaries eventName] event))



(defn- maybe-emit-session-event
  [state
   {:keys [sourceId eventName] :as event}
   {:keys [start-gate-name name-fn merge-with] :as spec}]

  (when-let [event1 (kv/get state sourceId [:session-boundaries (start-gate-name eventName)])]
    [;; closing gate
     (kv/del state sourceId [:session-boundaries (start-gate-name eventName)])
     ;; return the session event
     [(merge-with ;; merging function in spec
        (name-fn (start-gate-name eventName))
        event1
        event)]]))



(defcorrelate session-boundaries-correlation
  [state {:keys [sourceId timestamp eventName] :as event}]
  (when-let [{:keys [spec type]} (is-boundary-event? event)]
    (case type
      :start [(mark-session-start state event spec) []]
      :stop  (maybe-emit-session-event state event spec))))



(comment
  (def ev1
    {:timestamp 1436625946940
     :eventName "game.play.started"
     :sourceId  "device1"})

  (def ev2
    {:timestamp 1436626254608
     :eventName "game.play.stopped"
     :sourceId  "device1"})


  (def processor (moebius session-boundaries-correlation))
  (def state (kv/make-in-memory-kvstore))
  (processor state [ev1 ev2])
  )
