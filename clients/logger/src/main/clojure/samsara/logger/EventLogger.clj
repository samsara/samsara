(ns samsara.logger.EventLogger
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory])
  (:gen-class :constructors {[String String] []}
              :state state
              :init init
              :methods [[log4j2Event [Keyword String Throwable]]
                        [slf4jEvent [Keyword String Throwable]]]))

(def ALL_LEVEL :all)
(def TRACE_LEVEL :trace)
(def DEBUG_LEVEL :debug)
(def INFO_LEVEL :info)
(def WARN_LEVEL :warn)
(def ERROR_LEVEL :error)


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


(defn- log->samsara-event [{:keys [timestamp eventName]
                            :or {timestamp (System/currentTimeMillis)
                                 eventName "UnknownEvent"
                                 :as log}]
                           (assoc log :timestamp timestamp :eventName eventName))

;;IMPORTANT MAKE SURE EACH LOGGER ONLY CREATES ONE OF THIS CLASS .. WE DON'T WANT MULTIPLE loggers spamming samsara api url
;;ALSO check that the source id is send to this classes constructor

(defn- -init [^String api-url ^String source-id]
  (let [conf {:url api-url :sourceId source-id}]
    (when-not (empty? api-url)
      (slc/set-config conf))
    [[] conf]))




(defn- send-log [conf m]
  (if (empty? (:url conf))
    (println "*SAMSARA* " m)
    (do
      (let [log (assoc m :sourceId (:sourceId conf) :appId default-app-id)]
        (cli/record-event (log->samsara-event log))))))


(defn -log4j2Event [this log-level ^String msg ^Throwable t]
  (send-log (.state this) {:eventName "log"
                             :loggingFramework "Log4j2"
                             :level log-level
                             :message msg
                             :throwable t}))


(defn -slf4jEvent [this log-level ^String msg ^Throwable t]
  (send-log (.state this) {:eventName "log"
                             :loggingFramework "SLF4J"
                             :level log-level
                             :message msg
                             :throwable t}))
