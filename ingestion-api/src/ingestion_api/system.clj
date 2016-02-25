(ns ingestion-api.system
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [reloaded.repl :refer [system init start stop go reset]]
            [ingestion-api.components.backend :as backend]
            [ingestion-api.input.http :as http]
            [ingestion-api.input.http-admin :as admin]
            [ingestion-api.components.mqtt-server :as mqtt]
            [ingestion-api.core.processors :as ps]
            [ingestion-api.backend.api :refer :all]
            [samsara.trackit :refer [track-time track-distribution]]))


(defn send!
  [backend events]
  (track-time "ingestion.events.backend-send"
              (send backend events)))

(defn process-events [events-seq & {:keys [posting-timestamp]}]
  "Takes a sequence of events (and optional values) processes them and sends
   on to the backend,
   returns a map with the :status (:success or :error)
   and optionally :error-msg
   e.g
   {:status :success}

   {:status :error
    :error-msg (\"OK\" {:timestamp (not (integer? \"not a timestamp\"))})}
  "
  (if-let [errors (ps/is-invalid? events-seq)]
    (hash-map :status :error :error-msg (map #(if % % "OK") errors))
    (do
      (track-distribution "ingestion.payload.size" (count events-seq))
      (as-> events-seq $$
        (ps/inject-receivedAt (System/currentTimeMillis) $$)
        (ps/inject-publishedAt posting-timestamp $$)
        (send! (-> system :backend :backend) $$)
        (hash-map :status :success)))))


(defn ingestion-api-system
  [config]
  (component/system-map
   :backend     (backend/new-backend config)
   :admin-server (admin/new-admin-server config)
   :http-server (component/using
                 (http/new-http-server (assoc-in config [:server :process-fn] process-events))
                 {:backend :backend})
   :mqtt-server (component/using
                 (mqtt/new-mqtt-server config) {:backend :backend})))
