(ns samsara-builtin-modules.sessionize
  (:require [moebius.core :refer :all]
            [moebius.kv :as kv])
  (:require [digest :refer [sha-256]])
  (:require [samsara-builtin-modules.session-boundaries
             :refer [session-boundaries-correlation]]))

;; TODO: which event? and which key?
;; TODO: add sessionId on every requests
;; TODO: add dwell time
;; TODO: generate session.started/stopped
;; TODO: cutoff line for unfinished sessions (start without a stop)


(def *max-idle-time* 20)
(def *attribute* nil)
(def *web-event-name* "web.page.viewed")
(def *session-event-basename* "web.session")



(defenrich cleanup-prev-event
  [event]
  (dissoc event ::prev-event))



(defenrich add-sessionId
  [state {:keys [sourceId timestamp eventName] :as event}]
  (when-event-name-is
   event *web-event-name*

   (let [attr (get event *attribute*)
         key  (if attr [:sessionize attr] [:sessionize])
         {:keys [sessionId] :as prev} (kv/get state sourceId key)

         sessionId (if (and sessionId
                            (< 0 (- timestamp (:timestamp prev)) *max-idle-time*))
                     sessionId
                     (sha-256 (str timestamp "/" eventName "/" sourceId)))

         sevent (-> event
                    (inject-as :sessionId sessionId)
                    (inject-as ::prev-event (cleanup-prev-event prev)))

         new-state (kv/set state sourceId key sevent)]
     [new-state sevent])))



(defenrich identify-session-start
  [{:keys [sessionId eventName ::prev-event timestamp] :as event}]
  (when (and sessionId (= eventName *web-event-name*)
           (or (not prev-event)
              (> (- timestamp (:timestamp prev-event)) *max-idle-time*)))
    (inject-as event :isSessionStart true)))



(defcorrelate emit-session-start-and-stop
  [{:keys [isSessionStart ::prev-event eventName] :as event}]
  (when (and isSessionStart (= eventName *web-event-name*))
    (let [stop
          (when prev-event
            (-> (cleanup-prev-event prev-event)
                (inject-as :eventName (str *session-event-basename* ".stopped"))
                (inject-as :inferred true)
                (dissoc :isSessionStart)))
          start
          (-> (cleanup-prev-event event)
              (inject-as :eventName (str *session-event-basename* ".started"))
              (inject-as :inferred true)
              (dissoc :isSessionStart))]
      (if stop
        [stop start]
        [start]))))



(defcorrelate calculate-dwell-time
  [{:keys [sessionId dwellTime ::prev-event timestamp] :as event}]
  (when (and prev-event (not dwellTime)
           (= sessionId (:sessionId prev-event)))
    [(inject-as prev-event :dwellTime
                (- timestamp (:timestamp prev-event)))]))



(def sessionize-web-requests
  (pipeline
   add-sessionId
   identify-session-start
   emit-session-start-and-stop
   calculate-dwell-time
   cleanup-prev-event
   session-boundaries-correlation))


(comment
  (def events [{:eventName "web.page.viewed" :timestamp 1  :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 10 :sourceId "cookie1"}
               #_{:eventName "web.page.viewed" :timestamp 50 :sourceId "cookie1"}])

  (def events [{:eventName "web.page.viewed" :timestamp 1  :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 10 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 25 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 50 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 60 :sourceId "cookie1"}])

  (def proc (moebius sessionize-web-requests))

  (second
   (proc (kv/make-in-memory-kvstore) events) )

  )
