(ns ingestion-api.components.http-server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.reload :as reload]
            [aleph.http :refer [start-server]]
            [ingestion-api.route :refer [app]]))

(defn wrap-app
  [auto-reload]
  (if auto-reload
    (reload/wrap-reload #'app)
    app))

(defrecord HttpServer [port auto-reload server]
  component/Lifecycle

  (start [component]
    (clojure.pprint/pprint component)
    (if server (println "ALREADY STARTED") (println "STOPPED.."))
    (if server component 
        (as-> (wrap-app auto-reload) $
          (start-server $ {:port port})
          (assoc component :server $))))

  (stop [component]
    (println "stopping http >>>" (clojure.pprint/pprint component))
    (if server
      (->> component :server .close (assoc component :server))
      component)))

(defn new-http-server
  [config]
  (map->HttpServer (:server config)))
