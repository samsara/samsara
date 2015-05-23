(ns samsara.logger.core
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory]))

(def ^:private samsara-config (atom nil))

(def ^:private hostname (try
                          (.getCanonicalHostName (InetAddress/getLocalHost))
                          (catch UnknownHostException uhe
                            "UnknownHost")))

(def ^:private init-class (-> (Thread/currentThread)
                              (.getStackTrace)
                              last
                              (.getClassName)))

(def ^:private pid (-> (ManagementFactory/getRuntimeMXBean)
                       (.getName)
                       (.split "@")
                       first))

(def ^:private default-app-id (str hostname "-" init-class "-" pid ))

(defn- event->samsara-event [{:keys [timestamp sourceId eventName appId]
                                   :or {timestamp (System/currentTimeMillis)
                                        sourceId (:sourceId @samsara-config)
                                        eventName "UnknownEvent"
                                        appId (or (:appId @samsara-config) default-app-id)}
                                   :as event}]
  (assoc event :timestamp timestamp :sourceId sourceId :eventName eventName :appId appId))

;;TODO validate the configuration
(defn set-config [m]
  (when-not @samsara-config
    (reset! samsara-config m)
    (cli/init! m)))

(defn send-event [e]
  (if @samsara-config
    (cli/record-event (event->samsara-event e))
    (println "*SAMSARA* " e)))

