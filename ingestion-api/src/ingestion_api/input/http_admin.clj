(ns ingestion-api.input.http-admin
  (:require [aleph.http :refer [start-server]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [ingestion-api.input.route-util
             :refer
             [catch-all not-found wrap-reload wrap-app]]
            [ingestion-api.status :refer [change-status! is-online?]]
            [ring.middleware.json :as json]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;            ---==| H T T P   A D M I N   E N D P O I N T |==----            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;
;; This contains the routes which are reachable only by the admin network
;;
;; GET  /api-status   - health-check url to be used by the load balancer
;; PUT  /api-status   - to check the health-check status from 200 to 503
;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                         ---==| R O U T E S |==----                         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn admin-routes []

  (routes
   (context "/v1" []

            (GET "/api-status" []
                 {:status (if (is-online?) 200 503)
                  :body {:status (if (is-online?) "online" "offline")}})

            (PUT "/api-status" {{new-status :status} :body}
                 (if-not (= :error (change-status! new-status))
                   {:status 200 :body nil}
                   {:status 400 :body nil})))
   (not-found)))



(defn admin-app []
  (-> (admin-routes)
      (json/wrap-json-body {:keywords? true})
      (json/wrap-json-response)
      catch-all))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;           ---==| H T T P   A D M I N   C O M P O N E N T |==----           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defrecord AdminServer [port auto-reload server]
  component/Lifecycle

  (start [component]
    (log/info "Samsara Admin listening on port:" port)
    (if server
      component
      (as-> (wrap-app #(#'admin-app) auto-reload) $
        (start-server $ {:port port})
        (assoc component :server $))))

  (stop [component]
    (if server
      (update component :server #(.close %))
      component)))


(defn new-admin-server
  [config]
  (let [server-config (:admin-server config)]
    (map->AdminServer server-config)))
