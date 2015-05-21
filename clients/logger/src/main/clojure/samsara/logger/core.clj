(ns samsara.logger.core
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory]))

(def ^:private hostname (try
                          (.getCanonicalHostName (InetAddress/getLocalHost))
                          (catch UnknownHostException uhe
                            "UnknownHost")))

(def ^:private pid (-> (ManagementFactory/getRuntimeMXBean)
                       (.getName)
                       (.split "@")
                       first))

(def ^:private default-app-id (str hostname "-" pid ))

(defn- event->samsara-event [conf {:keys [timestamp sourceId eventName appId]
                                   :or {timestamp (System/currentTimeMillis)
                                        sourceId (:sourceId conf)
                                        eventName "UnknownEvent"
                                        appId (or (:appId conf) default-app-id)}
                                   :as event}]
  (assoc event :timestamp timestamp :sourceId sourceId :eventName eventName :appId appId))


(defn send-event [conf m]
  (if (not-empty (:url conf))
    (do
      (cli/set-config! conf)
      (cli/publish-events (event->samsara-event conf m)))
    (println "*SAMSARA* " m)))

