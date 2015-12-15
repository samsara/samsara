(ns samsara.client
  (:require [clj-http.client :as http]
            [com.stuartsierra.component :as component]
            [samsara
             [ring-buffer :refer :all]
             [utils :refer [to-json]]]
            [schema.core :as s]))

(def ^:const PUBLISHED-TIMESTAMP "X-Samsara-publishedTimestamp")


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

  "

  ([url events]
   (publish-events url events {:send-timeout-ms 30000}))

  ([url events {:keys [send-timeout-ms]
                :or {send-timeout-ms 30000} :as opts}]

   ;; validate events
   (when-let [errors (validate-events :batch events)]
     (throw (ex-info "Invalid events found." {:validation-error errors})))

   ;; POST them to ingestion-api
   (send-events (str url "/events")
                ;; headers
                {"Content-Type" "application/json"
                 ;; add pusblisjedTimestamp
                 PUBLISHED-TIMESTAMP
                 (str (System/currentTimeMillis))}
                ;; body
                (to-json events)
                opts)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ---==| R E C O R D - E V E N T |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn record-event-in-buffer
  "TODO: doc needed"
  [buffer event]

  ;; validate events
  (when-let [errors (validate-events :single event)]
    (throw (ex-info "Invalid events found." {:validation-error errors})))

  ;; add the event to the local buffer
  (enqueue buffer event))



(defn- flush-buffer
  "Flushes the event buffer to samsara api."
  [{:keys [url] :as config} buffer]
  (let [events (snapshot buffer)]
    (if (seq events)
      (try
        (publish-events url (map second events) config)
        (dequeue buffer events)
        (catch Throwable t
          buffer))
      buffer)))



(defrecord SamsaraClient [config]

  component/Lifecycle

  (start [this]
    (if (:buffer this)
      this
      (-> this
          (assoc :buffer (atom (ring-buffer (:max-buffer-size config)))))))

  (stop [this]
    (if-not (:buffer this)
      this
      (-> this
          (dissoc :buffer)))))


(defn samsara-client [config]
  (SamsaraClient. config))


(defn record-event!
  [{:keys [buffer] :as client} event]
  {:pre [buffer]}
  (last
   (items
    (swap! buffer record-event-in-buffer event))))





(comment
  (def rb (ring-buffer 10))

  (def rb (record-event rb {:eventName "a" :timestamp 1 :sourceId "d1"}))

  (map second (snapshot rb))

  (flush-buffer {:url "http://localhost:9000/v1"} rb)


  (def c (component/start (samsara-client {:max-buffer-size 3})))

  (def k (record-event! c {:eventName "a" :timestamp 3 :sourceId "d1"}))



  )
