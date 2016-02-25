(ns ingestion-api.input.http
  (:require [taoensso.timbre :as log])
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.reload :as reload]
            [aleph.http :refer [start-server]])
  (:require [ingestion-api.route-util :refer
             [gzip-req-wrapper catch-all not-found]])
  (:require [reloaded.repl :refer [system]])
  (:require [samsara.trackit :refer [track-distribution]])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [ring.util.response :refer :all :exclude [not-found]])
  (:require [ingestion-api.status :refer [is-online?]]
            [ingestion-api.events :refer [send! is-invalid? inject-receivedAt
                                          inject-publishedAt]]) )


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


(defn warn-when-missing-header [postingTimestamp]
  (when-not postingTimestamp
    {:status "OK"
     :warning "For completeness, please provide the 'X-Samsara-publishedTimestamp' header."}))



(defn- to-long
  "Convert a string to a number when possible, nil otherwhise."
  [^String num]
  (when num
    (try
      (Long/parseLong num)
      (catch Exception x nil))))



(defroutes app-routes

  (context "/v1" []

    (GET "/api-status" []
      {:status (if (is-online?) 200 503)
       :body {:status (if (is-online?) "online" "offline")}})

    (POST  "/events"   {events :body
                        {postingTimestamp "x-samsara-publishedtimestamp"} :headers}

      (if-let [process-fn (-> system :http-server :process-fn)]
        (let [process-result (process-fn events :posting-timestamp (to-long postingTimestamp))]
          (if (= :success (:status process-result))
            {:status 202
             :body (warn-when-missing-header postingTimestamp)}
            {:status 400
             :body (:error-msg process-result)}))
        (do
          (log/error "The http-server component wasn't configured with a process-fn"
                    (-> system :http-server))
          {:status 500
             :body "Server misconfiguration"}))))
  (not-found))


(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (gzip-req-wrapper)
      catch-all))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| H T T P   C O M P O N E N T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn wrap-app
  [auto-reload]
  (if auto-reload
    (do
      (log/info "AUTO-RELOAD enabled!!! I hope you are in dev mode.")
      (reload/wrap-reload #'app))
    app))


(defrecord HttpServer [port auto-reload backend server process-fn]
  component/Lifecycle


  (start [component]
    (log/info "Samsara Ingestion-API listening on port:" port)
    (if server component
        (as-> (wrap-app auto-reload) $
          (start-server $ {:port port})
          (assoc component :server $))))

  (stop [component]
    (if server
      (update component :server #(.close %))
      component)))



(defn new-http-server
  [config]
  (map->HttpServer (:server config)))
