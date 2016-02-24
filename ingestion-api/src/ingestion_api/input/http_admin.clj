(ns ingestion-api.input.http-admin
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [aleph.http :refer [start-server]])
  (:require [ingestion-api.route-util :refer [catch-all not-found]])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [ring.util.response :refer :all :exclude [not-found]]
            [ingestion-api.status :refer [change-status! is-online?]]))



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


(defroutes admin-routes

  (context "/v1" []

           (GET "/api-status" []
                {:status (if (is-online?) 200 503)
                 :body {:status (if (is-online?) "online" "offline")}})

           (PUT "/api-status" {{new-status :status} :body}
                (if-not (= :error (change-status! new-status))
                  {:status 200 :body nil}
                  {:status 400 :body nil})))
  (not-found))



(def admin-app
  (-> admin-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      catch-all))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;           ---==| H T T P   A D M I N   C O M P O N E N T |==----           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def default-values {:port 9010})


(defrecord AdminServer [server-instance port]
  component/Lifecycle

  (start [component]
    (log/info "Samsara Admin listening on port:" port)
    (if server-instance component
        (->>  (start-server admin-app {:port port})
              (assoc component :server-instance))))

  (stop [component]
    (if server-instance
      (update component :server-instance #(.close %))
      component)))


(defn new-admin-server
  [config]
  (let [server-config (merge default-values (:admin-server config))]
    (map->AdminServer server-config)))
