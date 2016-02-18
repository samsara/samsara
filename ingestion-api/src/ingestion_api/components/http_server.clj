(ns ingestion-api.components.http-server
  (:require [taoensso.timbre :as log])
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.reload :as reload]
            [aleph.http :refer [start-server]]
            [ingestion-api.route :refer [app]]))

(defn wrap-app
  [auto-reload]
  (if auto-reload
    (do
      (log/info "AUTO-RELOAD enabled!!! I hope you are in dev mode.")
      (reload/wrap-reload #'app))
    app))

(defrecord HttpServer [port auto-reload backend server]
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
