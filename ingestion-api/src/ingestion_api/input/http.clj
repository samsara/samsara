(ns ingestion-api.input.http
  (:refer-clojure :exclude [send])
  (:require [aleph.http :refer [start-server]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [ingestion-api.backend.backend :refer [send]]
            [ingestion-api.core.processors :refer [process-events]]
            [ingestion-api.input.route-util
             :refer
             [catch-all wrap-gzip-requests not-found wrap-reload wrap-app]]
            [ingestion-api.status :refer [is-online?]]
            [ring.middleware.json :as json]
            [ring.util.response :refer :all :exclude [not-found]]
            [samsara.trackit :refer [track-distribution track-rate]]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ---==| H T T P   E N D P O I N T |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; This contains the routes which are reachable by the clients
;;
;; POST /events       - to submit a bunch of events
;; GET  /api-status   - health-check url to be used by the load balancer
;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                         ---==| R O U T E S |==----                         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn warn-when-missing-header [publishedTimestamp]
  (when-not publishedTimestamp
    {:status "OK"
     :warning "For completeness, please provide the 'X-Samsara-publishedTimestamp' header."}))



(defn- to-long
  "Convert a string to a number when possible, nil otherwhise."
  [^String num]
  (when num
    (try
      (Long/parseLong num)
      (catch Exception x nil))))



(defn when-json-content-type
  "If the request is has not a json content type it returns a error"
  [handler]
  (fn [request]
    (if-not (re-matches #"^application/(.+\+)?json" (get-in request [:headers "content-type"] ""))

      ;; the payload is not JSON
      {:status 400
       :body {:status :ERROR
              :message "Invalid format, content-type must be application/json"}}

      (handler request))))



(defn with-publishedTimestamp-header
  "Ensures that the publishedTimestamp header is a valid timestamp"
  [handler]
  (fn [{{publishedTimestamp "x-samsara-publishedtimestamp"} :headers :as request}]
    (if (and publishedTimestamp (nil? (to-long publishedTimestamp)))
      {:status 400
       :body {:status :ERROR
              :message "X-Samsara-publishedTimestamp must be a valid timestamp."}}
      (handler request))))


(defn- send-to-backend
  [backend events]
  (track-rate "ingestion.http.requests")
  (track-rate "ingestion.http.events" (count events))
  (track-distribution "ingestion.http.batch.size" (count events))
  (send backend events))



(defn app-routes [backend]

  (routes
   (context "/v1" []

            (GET "/api-status" []
                 {:status (if (is-online?) 200 503)
                  :body {:status (if (is-online?) "online" "offline")}})


            (when-json-content-type
                (with-publishedTimestamp-header
                  (POST  "/events"   {events :body
                                      {publishedTimestamp "x-samsara-publishedtimestamp"} :headers}

                         ;; if all events are valid then process and send them to the backend
                         (let [{:keys [status error-msgs processed-events]}
                               (process-events events :publishedTimestamp
                                               (to-long publishedTimestamp))]
                           (if (= :error status)
                             {:status 400 :body error-msgs}
                             (do
                               (send-to-backend backend processed-events)
                               {:status 202
                                :body (warn-when-missing-header publishedTimestamp)})))))))
   (not-found)))


(defn app [backend]
  (-> (app-routes backend)
      (json/wrap-json-body {:keywords? true})
      (json/wrap-json-response)
      (wrap-gzip-requests)
      catch-all))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| H T T P   C O M P O N E N T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defrecord HttpServer [port auto-reload backend server]
  component/Lifecycle


  (start [component]
    (log/info "Samsara Ingestion-API listening on port:" port)
    (log/info "BACKEND:" (:backend component))
    (if server component
        (as-> (wrap-app #(#'app (:backend component)) auto-reload) $
          (start-server $ {:port port})
          (assoc component :server $))))

  (stop [component]
    (if server
      (update component :server #(.close %))
      component)))



(defn new-http-server
  [{:keys [server]}]
  {:pre [(number? (:port server))]}
  (map->HttpServer server))
