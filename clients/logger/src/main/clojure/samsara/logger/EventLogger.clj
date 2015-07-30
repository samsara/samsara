(ns samsara.logger.EventLogger
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory]
           [org.slf4j.spi LocationAwareLogger]
           [org.apache.logging.log4j Level]
           )
  (:gen-class 
              :constructors {[String String] []}
              :state state
              :init init
              :methods [[log4j2Event [org.apache.logging.log4j.Level String Throwable] void]
                        [slf4jEvent [Integer String Throwable] void]]
              ))


(def slf4j-level-map {LocationAwareLogger/TRACE_INT :trace
                      LocationAwareLogger/DEBUG_INT :debug
                      LocationAwareLogger/INFO_INT  :info
                      LocationAwareLogger/WARN_INT  :warn
                      LocationAwareLogger/ERROR_INT :error})

(def log4j2-level-map {Level/ALL   :all
                       Level/TRACE :trace
                       Level/DEBUG :debug
                       Level/INFO  :info
                       Level/WARN  :warn
                       Level/ERROR :error
                       Level/FATAL :fatal})

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
                                 eventName "UnknownEvent"}
                                 :as log}]
                           (assoc log :timestamp timestamp :eventName eventName))

;;IMPORTANT MAKE SURE EACH LOGGER ONLY CREATES ONE OF THIS CLASS .. WE DON'T WANT MULTIPLE loggers spamming samsara api url
;;ALSO check that the source id is send to this classes constructor

(defn- -init [^String api-url ^String source-id]
  (let [conf {:url api-url :sourceId source-id}]
    (when-not (empty? api-url)
      (cli/init! conf))
    [[] conf]))




(defn- send-log [conf m]
  (if (empty? (:url conf))
    (println "*SAMSARA* " m)
    (do
      (let [log (assoc m :sourceId (:sourceId conf) :appId default-app-id)]
        (cli/record-event (log->samsara-event log))))))


(defn -log4j2Event [this ^Level log-level ^String msg ^Throwable t]
  (send-log (.state this) {:eventName "log"
                           :loggingFramework "Log4j2"
                           :level (get log4j2-level-map log-level)
                           :message msg
                           :throwable t}))


(defn -slf4jEvent [this ^Integer log-level ^String msg ^Throwable t]
  (send-log (.state this) {:eventName "log"
                           :loggingFramework "SLF4J"
                           :level (get slf4j-level-map log-level)
                           :message msg
                           :throwable t}))
