(ns samsara.client
  (:require [clj-http.client :as http]
            [com.stuartsierra.component :as component]
            [samsara
             [ring-buffer :refer :all]
             [utils :refer [to-json stoppable-thread gzip-string]]]
            [schema.core :as s]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ---==| C O N F I G U R A T I O N |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:const PUBLISHED-TIMESTAMP "X-Samsara-publishedTimestamp")

(def ^:const DEFAULT-CONFIG
  {
   ;; a samsara ingestion api endpoint  "http://samsara.io/"
   ;; :url  - REQUIRED

   ;; the identifier of the source of these events
   ;; :sourceId  - OPTIONAL only for record-event

   ;; whether to start the publishing thread.
   :start-publishing-thread true

   ;; how often should the events being sent to samsara
   ;; in milliseconds
   ;; default 30s
   :publish-interval 30000

   ;; max size of the buffer, when buffer is full,
   ;; older events are dropped.
   :max-buffer-size  10000

   ;; minimum number of events to that must be in the buffer
   ;; before attempting to publish them
   :min-buffer-size 100


   ;; network timeout for send operaitons (in millis)
   ;; default 30s
   :send-timeout-ms  30000

   ;; whether of not the payload should be compressed
   ;; allowed values :gzip :none
   :compression :gzip

   ;; add samsara client statistics events
   ;; this helps you to understand whether the
   ;; buffer size and publish-intervals are
   ;; adequately configured.
   ;; :send-client-stats true
   })


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                ---==| V A L I D A T E - E V E N T S |==----                ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^{:private true} event-schema
  "Schema to validate a single event"
   {(s/required-key :timestamp) s/Int
     (s/required-key :sourceId)  s/Str
     (s/required-key :eventName) s/Str
     s/Keyword s/Any})



(defn validate-events
  "It validate a single event or a batch of events.
   If the validation is successful `nil` is returned,
   if validation fails a data structure with the reason
   of the failure is returned.
   `single-or-batch` can be either `:single` or `:batch`"
  [single-or-batch events]
  (condp = single-or-batch
    :single (s/check event-schema   events)
    :batch  (s/check [event-schema] events)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| P U B L I S H - E V E N T S |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- send-events
  "Makes an HTTP request to the service endpoint specified by 'url'
   and posts the given events. If successful returns the HTTP response."
  [url headers body {:keys [send-timeout-ms] :or {send-timeout-ms 30000}}]
  ;; post to ingestion-api
  (http/post url
             ;; set timeout
             {:socket-timeout send-timeout-ms :conn-timeout send-timeout-ms
              ;; expected response format
              :accept :json :as :json
              ;; payload format
              :headers headers
              ;; events payload
              :body body}))



(defn publish-events

  "Takes a collection of events and publishes to samsara immediately.
   If successful returns the HTTP server response, otherwise it raises
   an exception.

   `url`    - the base endpoint url such as http://localhost:9000/v1
   `events` - a list or vector of valid events

   options:

   `:send-timeout-ms` (default 30000) - the time to wait for a server
       response before to time out.

   `compression` (default :gzip) - whether to compress or not the
       payload. Valid values are :gzip and :none
  "

  ([url events]
   (publish-events url events nil))

  ([url events {:keys [send-timeout-ms compression]
                :or {send-timeout-ms 30000
                     compression :gzip} :as opts}]

   ;; validate events
   (when-let [errors (validate-events :batch events)]
     (throw (ex-info "Invalid events found." {:validation-error errors})))

   (let [encode (if (= :gzip compression) (comp gzip-string to-json) to-json)]

     ;; POST them to ingestion-api
     (send-events (str url "/v1/events")
                  ;; headers
                  {"Content-Type" "application/json"
                   "Content-Encoding" (if (= :gzip compression) "gzip" "identity")
                   ;; add pusblisjedTimestamp
                   PUBLISHED-TIMESTAMP
                   (str (System/currentTimeMillis))}
                  ;; body
                  (encode events)
                  opts))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ---==| R E C O R D - E V E N T |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- record-event-in-buffer
  "Normalizes and validates the event and it add it to the buffer.
  Returns the new buffer"
  [buffer event]

  ;; validate events
  (when-let [errors (validate-events :single event)]
    (throw (ex-info "Invalid events found." {:validation-error errors})))

  ;; add the event to the local buffer
  (enqueue buffer event))



(defn- flush-buffer
  "Flushes the event buffer to samsara api."
  [{:keys [url] :as config} buffer
   & {:keys [on-success] :or {on-success (fn [buffer events]
                                           (dequeue buffer events))}}]
  (let [events (snapshot buffer)]
    (if (seq events)
      (try
        (publish-events url (map second events) config)
        (on-success buffer events)
        (catch Throwable t
          buffer))
      buffer)))



(defn record-event!

  "It record the given event into a local buffer to be sent
   later at regular interval. Returns the event as it was
   added to the buffer."

  [{:keys [buffer] :as client} event]
  {:pre [buffer]}
  (last
   (items
    (swap! buffer record-event-in-buffer event))))



(defn flush-buffer!

  "Sends the content of the buffer to the ingestion endpoint.
  If successful it removes the events which where sent from
  the buffer."

  [{:keys [buffer config] :as client}]
  {:pre [buffer]}
  (flush-buffer config @buffer
                :on-success (fn [_ events]
                              (swap! buffer dequeue events))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| C L I E N T   C O M P O N E N T |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- flush-buffer-if-ready
  [{buffer :buffer
    {:keys [min-buffer-size]} :config
    :as client}]
  (when (>= (count @buffer) min-buffer-size)
    (flush-buffer! client)))


(defn- background-flush-buffer-thread
  "When the publishing-thread is active it starts a background thread
   which at regular interval will flush the buffer.
   It returns a function which when called it stop the thread."
  [{{:keys [start-publishing-thread publish-interval]} :config :as client}]
  (if start-publishing-thread
    (stoppable-thread "Samsara-Client-Flush-Buffer"
     (fn []
       (flush-buffer-if-ready client))
     :sleep-time publish-interval)
    (fn [])))


(defrecord SamsaraClient [config]

  component/Lifecycle

  (start [this]
    (if (:buffer this)
      this
      (as-> this $
        (assoc $ :buffer (atom (ring-buffer (:max-buffer-size config))))
        (assoc $ :flush-thread (background-flush-buffer-thread $)))))

  (stop [this]
    (if-let [buffer (:buffer this)]
      (let [buf @buffer
            flush-thread (:flush-thread this)]
        ;; stop background thread
        (flush-thread)
        ;; nil buffer to avoid new events in
        (swap! buffer (constantly nil))
        ;; flush the current content of the buffer
        (flush-buffer config buf)
        ;; stop the client
        (dissoc this :buffer))
      this)))



(defn- sanitize-configuration
  [{:keys [url max-buffer-size min-buffer-size publish-interval] :as config}]
  "Checks and attempts to correct invalid configuration when possible.
   It return a patched and valid configuration or throws an error."
  (when-not url
    (throw (ex-info "Missing Samsara's ingestion api endpoint url." config)))

  (when (<= publish-interval 0)
    (throw (ex-info "Invalid interval time for Samsara client." config)))

  (as-> config $
    (if (> min-buffer-size max-buffer-size)
      (assoc $ :min-buffer-size 1) $)
    (if (= 0 min-buffer-size)
      (assoc $ :min-buffer-size 1) $)))



(defn samsara-client
  "It creates a samsara client component with the given configuration"
  [{:keys [url] :as config}]
  (SamsaraClient.
   (sanitize-configuration
    (merge DEFAULT-CONFIG config))))




(comment

  (def c (component/start
          (samsara-client
           {:url "http://localhost:9000"
            :max-buffer-size 3
            :publish-interval 3000})))

  (def k (record-event! c {:eventName "a" :timestamp 2 :sourceId "d1"}))

  (count @(:buffer c))

  (component/stop c)

  )
