(ns ingestion-api.input.http
  (:refer-clojure :exclude [send])
  (:require [taoensso.timbre :as log])
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.reload :as reload]
            [aleph.http :refer [start-server]])
  (:require [ingestion-api.input.route-util :refer
             [gzip-req-wrapper catch-all not-found]])
  (:require [reloaded.repl :refer [system]])
  (:require [samsara.trackit :refer [track-distribution]])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [ring.util.response :refer :all :exclude [not-found]])
  (:require [ingestion-api.status :refer [is-online?]]
            [ingestion-api.core.processors :refer [process-events]]
            [ingestion-api.backend.backend :refer [send]]) )


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



(defn app-routes [backend]

  (routes
   (context "/v1" []

            (GET "/api-status" []
                 {:status (if (is-online?) 200 503)
                  :body {:status (if (is-online?) "online" "offline")}})


            ;; TODO: maybe yada could help here with content-type
            (POST  "/events"   {events :body
                                {publishedTimestamp "x-samsara-publishedtimestamp"
                                 content-type       "content-type"} :headers}

                   ;; regex taken from ring-json middleware
                   (if-not (re-matches #"^application/(.+\+)?json" content-type)

                     ;; the payload is not JSON
                     {:status 400
                      :body {:status :ERROR
                             :message "Invalid format, content-type must be application/json"}}

                     ;; the payload a valid JSON, let's validate the events
                     (let [process-result (process-events events :publishedTimestamp
                                                          (to-long publishedTimestamp))
                           {:keys [status error-msgs processed-events]} process-result]
                       (if (= :error status)
                         {:status 400 :body error-msgs}
                         (do
                           (track-distribution "ingestion.payload.size" (count events))
                           (send backend processed-events)
                           {:status 202
                            :body (warn-when-missing-header publishedTimestamp)}))))))
   (not-found)))


(defn app [backend]
  (-> (app-routes backend)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (gzip-req-wrapper)
      catch-all))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| H T T P   C O M P O N E N T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: RELOAD won't probably work
(defn wrap-app
  [app-fn auto-reload]
  (if auto-reload
    (do
      (log/info "AUTO-RELOAD enabled!!! I hope you are in dev mode.")
      (reload/wrap-reload (app-fn)))
    (app-fn)))


(defrecord HttpServer [port auto-reload backend server]
  component/Lifecycle


  (start [component]
    (log/info "Samsara Ingestion-API listening on port:" port)
    (log/info "BACKEND:" (:backend component))
    (if server component
        (as-> (wrap-app #(app (:backend component)) auto-reload) $
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
