(ns samsara.client2
  (:require [samsara.utils :refer [to-json]])
  (:require [schema.core :as s])
  (:require [clj-http.client :as http]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| P U B L I S H   E V E N T S |==----                 ;;
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

  ([url events & {:keys [send-timeout-ms]
                  :or {send-timeout-ms 30000} :as opts}]

   (when-let [errors (validate-events :batch events)]
     (throw (ex-info "Invalid events found." {:validation-error errors})))
   (send-events (str url "/events")
                ;; headers
                {"Content-Type" "application/json"
                 ;; add pusblisjedTimestamp
                 "X-Samsara-publishedTimestamp"
                 (str (System/currentTimeMillis))}
                ;; body
                (to-json events)
                opts)))


(comment
  (publish-events "http://localhost:9000/v1"
   [{:eventName "a" :timestamp 1 :sourceId "a"}
    {:eventName "b" :timestamp 2 :sourceId "b"}])
  )
