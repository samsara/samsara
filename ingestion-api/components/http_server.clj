(ns ingestion-api.http-server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.reload :as reload]
            [aleph.http :refer [start-server]]
            [ingestion-api.route :refer [app]]))

(defn wrap-app
  [auto-reload]
  (if auto-reload
    (reload/wrap-reload #'app)
    app))

(defrecord HttpServer [port auto-reload]
  component/Lifecycle

  (start [component]
    (as-> (wrap-app auto-reload)  $
        (start-server $ {:port port})
        (assoc component :http-server $)))

  (stop [component]
    (some->>
     component
     (:http-server)
     (.close)
     (assoc component :http-server))))

(defn new-http-server [config]
  (map->HttpServer (:server config)))
