(ns samsara.logger.core
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]))

(def ^:private default-sourceId (str "hydrant-"
                                     (try
                                       (.getCanonicalHostName (InetAddress/getLocalHost))
                                       (catch UnknownHostException uhe
                                         "UnknownHostException"))))

(def ^:private default-config {:url (System/getenv "SAMSARA_API_URL")
                               :sourceId (or (System/getenv "SAMSARA_SOURCE_ID") default-sourceId)})

(defn- event->samsara-event [{:keys [timestamp sourceId eventName]
                             :or {timestamp (System/currentTimeMillis)
                                  sourceId default-sourceId
                                  eventName "UnknownEvent"}
                             :as event}]
  (assoc event :timestamp timestamp :sourceId sourceId :eventName eventName))


(def url-configured? 
  (delay
   (let [url (:url default-config)]
     (if url
       (cli/set-config! default-config)
       (do
         (println "****************************************************************")
         (println "SAMSARA: The environment variable \"SAMSARA_API_URL\" is not set")
         (println "SAMSARA: Samsara SLF4J logger will just print to console")
         (println "****************************************************************\n")))
     url)))



(defn send-event [m]
  (if-let [url @url-configured?]
    (cli/publish-events (event->samsara-event m))
    (println "*SAMSARA* " m)))

