(ns samsara.logger.core
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]))


(defn- event->samsara-event [conf {:keys [timestamp sourceId eventName]
                                   :or {timestamp (System/currentTimeMillis)
                                        sourceId (:sourceId conf)
                                        eventName "UnknownEvent"}
                                   :as event}]
  (assoc event :timestamp timestamp :sourceId sourceId :eventName eventName))


(defn send-event [conf m]
  (if (not-empty (:url conf))
    (do
      (cli/set-config! conf)
      (cli/publish-events (event->samsara-event conf m)))
    (println "*SAMSARA* " m)))

