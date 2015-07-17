(ns samsara-builtin-modules.sessionize
  (:require [moebius.core :refer :all]
            [moebius.kv :as kv])
  (:require [digest :refer [sha-256]])
  (:require [samsara-builtin-modules.session-boundaries
             :refer [session-boundaries-correlation]]))


(def DEFAULT-CONFIG
  {;; :max-idle-time is max time in milliseconds which can elapse
   ;; between two web calls from the same user to consider them part
   ;; of the same web session.
   ;;
   ;; (default): 20 min
   :max-idle-time  (* 20 60 1000) ;; 20 minutes

   ;; :attribute is the name of the event attribute which together
   ;; with the :sourceId form the guid (general user identifier). This
   ;; is typically a value of a cookie which is assigned at the first
   ;; access by the LB or server and used to keep track of new or
   ;; returning users.  By default it uses only the sourceId, it
   ;; doesn't use any additional attribute (`nil`). Remember that if
   ;; you specify another attribute this will be used as composition
   ;; with the sourceId and not as a replacement. This is due to the
   ;; fact that the sourceId is the partition key, and the only way to
   ;; define how data is distributed across processing machines. So if
   ;; your identifier is in another field then you won't be guaranteed
   ;; that all events from that specific user will be processed
   ;; by the same processor thread. In that case you have to switch the
   ;; attributes and put your GUID into sourceId.
   ;; (default): nil
   :attribute      nil

   ;; :web-event-name name of the event which contains the page views.
   ;; only these events will be considered for the processing
   ;;
   ;; (default): "web.page.viewed"
   :web-event-name "web.page.viewed"

   ;; :session-event-basename is the base name for the session events.
   ;; When a session start a new event will be generated with the
   ;; following name `${session-event-basename}.started`, similarly
   ;; when the session ends a event called
   ;; `${session-event-basename}.stopped` is generated.  This
   ;; parameter enables you to customise the name of the generated
   ;; event.
   ;;
   ;; (default): "web.session"
   :session-event-basename "web.session"})


(defenrich cleanup-prev-event
  [event]
  (dissoc event ::prev-event))



(defenrich* add-sessionId
  [{:keys [web-event-name attribute max-idle-time] :as cfg}]
  (fn [state {:keys [sourceId timestamp eventName] :as event}]
    (when-event-name-is
     event web-event-name

     (let [attr (get event attribute)
           key  (if attr [:sessionize attr] [:sessionize])
           {:keys [sessionId] :as prev} (kv/get state sourceId key)

           sessionId (if (and sessionId
                            (< 0 (- timestamp (:timestamp prev)) max-idle-time))
                       sessionId
                       (sha-256 (str timestamp "/" eventName "/" sourceId)))

           sevent (-> event
                      (inject-as :sessionId sessionId)
                      (inject-as ::prev-event (cleanup-prev-event prev)))

           new-state (kv/set state sourceId key sevent)]
       [new-state sevent]))))



(defenrich* identify-session-start [{:keys [web-event-name max-idle-time] :as cfg}]
  (fn [{:keys [sessionId eventName ::prev-event timestamp] :as event}]
    (when (and sessionId (= eventName web-event-name)
             (or (not prev-event)
                (> (- timestamp (:timestamp prev-event)) max-idle-time)))
      (inject-as event :isSessionStart true))))



(defcorrelate* emit-session-start-and-stop
  [{:keys [session-event-basename web-event-name] :as cfg}]
  (fn [{:keys [isSessionStart ::prev-event eventName] :as event}]
    (when (and isSessionStart (= eventName web-event-name))
      (let [stop
            (when prev-event
              (-> (cleanup-prev-event prev-event)
                  (inject-as :eventName (str session-event-basename ".stopped"))
                  (inject-as :inferred true)
                  (dissoc :isSessionStart)))
            start
            (-> (cleanup-prev-event event)
                (inject-as :eventName (str session-event-basename ".started"))
                (inject-as :inferred true)
                (dissoc :isSessionStart))]
        (if stop
          [stop start]
          [start])))))



(defcorrelate calculate-dwell-time
  [{:keys [sessionId dwellTime ::prev-event timestamp] :as event}]
  (when (and prev-event (not dwellTime)
           (= sessionId (:sessionId prev-event)))
    [(inject-as prev-event :dwellTime
                (- timestamp (:timestamp prev-event)))]))



(defn sessionize-web-requests
  ([cfg]
   (pipeline
    (add-sessionId cfg)
    (identify-session-start cfg)
    (emit-session-start-and-stop cfg)
    calculate-dwell-time
    cleanup-prev-event
    session-boundaries-correlation))
  ([]
   (sessionize-web-requests DEFAULT-CONFIG)))


(comment

  (def cfg {:max-idle-time 20,
            :attribute nil,
            :web-event-name "web.page.viewed",
            :session-event-basename "web.session"})


  (def events [{:eventName "web.page.viewed" :timestamp 1  :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 10 :sourceId "cookie1"}
               #_{:eventName "web.page.viewed" :timestamp 50 :sourceId "cookie1"}])

  (def events [{:eventName "web.page.viewed" :timestamp 1  :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 10 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 25 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 50 :sourceId "cookie1"}
               {:eventName "web.page.viewed" :timestamp 60 :sourceId "cookie1"}])

  (def proc (moebius (sessionize-web-requests cfg)))

  (second
   (proc (kv/make-in-memory-kvstore) events) )

  (require '[moebius.pipeline-utils :as g])

  (g/view-pipeline-graph (sessionize-web-requests cfg))
  )
