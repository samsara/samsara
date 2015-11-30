(ns samsara.client
  (:require [samsara.utils :refer [to-json]])
  (:require [taoensso.timbre :as log])
  (:require [org.httpkit.client :as http])
  (:require [samsara.ring-buffer :refer :all])
  (:require [schema.core :as s])
  (:require [chime :refer [chime-ch]]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [clojure.core.async :as a :refer [<!! thread]]))

(comment

;; Config
(def ^:dynamic *config* nil)
(def ^{:dynamic true :private true} !buffer! (atom nil))

(def DEFAULT-CONFIG
  {
   ;; a samsara ingestion api endpoint  "http://samsara.io/v1"
   ;; :url  - REQUIRED

   ;; the identifier of the source of these events
   ;; :sourceId  - REQUIRED only for record-event

   ;; whether to start the publishing thread.
   :start-publishing-thread true

   ;; how often should the events being sent to samsara
   :publish-interval 60               ;seconds

   ;; max size of the buffer, when buffer is full,
   ;; older events are dropped.
   :max-buffer-size  10000

   ;; minimum number of events to that must be in the buffer
   ;; before attempting to publish them
   ;; min-buffer-size 100


   ;; network timeout for send operaitons (in millis)
   :send-timeout-ms  3000

   ;; whether of not the payload should be compressed
   ;; allowed values :gzip :none
   ;; :compression :gzip

   ;; add samsara client statistics events
   ;; this helps you to understand whether the
   ;; buffer size and publish-intervals are
   ;; adequately configured.
   ;; :send-client-stats true
   })


(defn get-samsara-config [] *config*)

(defn set-config!
  "Set samsara configuration.
   The following properties can be set:
   :url - Samsara URL
   :sourceId - Unique ID for the client. This property is defaulted with an autogenerated value.
   :publish-interval - Frequency in seconds, in which the events will be flushed to samsara API.
   :max-buffer-size - Max size of the events ring buffer.

   NOTE: Changes to publish-interval and max-buffer-size properties will require a restart immediately
   after the first call to record-event"
  [config]
  (alter-var-root #'*config* (constantly config)))



(def single-event-schema
  "Schema validation for events"
  {
   (s/required-key :timestamp) s/Int
   (s/required-key :sourceId)  s/Str
   (s/required-key :eventName) s/Str
   s/Keyword s/Any})


;; this is the validation
;; for the entire payload
;; which it is just a composition of
;; other prismatic/schema (nice)
(def events-schema
  "Schema for a batch of events"
  [ single-event-schema ])


(defn- validate-event
  "Validates the event [or the list of events]
    and throws an Exception if Invalid"
  [events]
  (try
    (let [schema (if (map? events) single-event-schema events-schema)]
      (s/validate schema events))
    (catch clojure.lang.ExceptionInfo x
      ;; TODO: is this necessary? maybe we can just use the
      ;; prismatic/schema exception
      (throw (IllegalArgumentException. "Validation error" x)))))


(defn- enrich-event
  "Enriches the event with default properties etc."
  [event]
  (merge {:timestamp (System/currentTimeMillis)
          :sourceId (:sourceId *config*)}
         event))


(defn- prepare-event
  "Enriches and validates the event and throws an Exception if validation fails."
  [event]
  (let [e (enrich-event event)]
    (validate-event e)
    e))


(defn- send-events
  "Send events to samsara api"
  ([events]
   (send-events (merge DEFAULT-CONFIG *config*) events))
  ([{:keys [url send-timeout-ms]} events]
   (let [{:keys [status error] :as resp}
         @(http/post (str url "/events")
                     {:timeout send-timeout-ms
                      :headers {"Content-Type" "application/json"
                                "X-Samsara-publishedTimestamp" (str (System/currentTimeMillis))}
                      :body (to-json events)})]
     ;;Throw the exception from HttpKit to the caller.
     (when error
       (log/error "Failed to connect to samsara with error: " error)
       (throw error))
     ;;Throw an exception if status is not 2xx.
     (when (not (<= 200 status 299))
       (log/error "Publish failed with status:" status)
       (throw (RuntimeException. (str "PublishFailed with status=" status)))))))


(defn publish-events
  "Takes a vector containing events and publishes to samsara immediately."
  ([events]
   (validate-event events)
   (send-events events))
  ([url events & {:as opts}]
   (validate-event events)
   (send-events
    (merge DEFAULT-CONFIG opts {:url url})
    events)))


(defn- flush-buffer
  "Flushes the event buffer to samsara api. Does nothing if another flush-buffer is in progress."
  []
  (let [events (snapshot @!buffer!)]
    (if (seq events)
      (try
        (send-events (map second events))
        (swap! !buffer! dequeue! events)
        (catch Throwable t
          (log/error t "Flush failed. Leaving events in the buffer to try again.")))
      (log/info "Nothing to send."))))


(defn- init-timer! [{:keys [publish-interval start-publishing-thread] :as config}]
  (when start-publishing-thread
    (let [times (periodic-seq (t/now) (-> publish-interval t/seconds))]
      (log/info "Starting job to flush events.")
      (let [chimes (chime-ch times {:ch (a/chan (a/sliding-buffer 1))})]
        (thread
          (loop [] ;; TODO: add a way to stop the timer.
            (when-let [time (<!! chimes)]
              (log/info "Flushing buffer now.")
              (flush-buffer)
              (recur))))))))


(defn init! [config]
  (when-not *config*
    (let [cfg (merge DEFAULT-CONFIG config)
          ;; TODO: maybe validate the config
          ;; here to avoid silly config values
          ;; which would cause trouble
          ;; to the system and check that required stuff
          ;; are present.
          ;; maybe validation should be in set-config!
          ]
      ;; set the global config
      (set-config! cfg)
      ;; init buffer
      (compare-and-set! !buffer! nil (ring-buffer (:max-buffer-size *config*)))
      ;; start timer
      (init-timer! cfg)
      cfg)))


(defn record-event
  "Buffers the events to be published later."
  [event]
  (->> (prepare-event event)
      (swap! !buffer! enqueue!)
      (snapshot)
      last
      second))
)
