(ns samsara.logger.EventLogger
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory]
           [org.slf4j.spi LocationAwareLogger]
           [org.apache.logging.log4j Level]
           [clojure.lang IPersistentMap]
           [ch.qos.logback.classic.spi IThrowableProxy]
           )
  (:gen-class 
              :constructors {[String String] []
                             [String String Integer] []
                             [clojure.lang.IPersistentMap] []}
              :state state
              :init init
              :methods [[log4j2Event [org.apache.logging.log4j.Level String Throwable] void]
                        [slf4jEvent [Integer String Throwable] void]
                        [logbackEvent [ch.qos.logback.classic.Level String ch.qos.logback.classic.spi.IThrowableProxy] void]]
              ))


(def slf4j-level-map {LocationAwareLogger/TRACE_INT :trace
                      LocationAwareLogger/DEBUG_INT :debug
                      LocationAwareLogger/INFO_INT  :info
                      LocationAwareLogger/WARN_INT  :warn
                      LocationAwareLogger/ERROR_INT :error})

(def logback-level-map {ch.qos.logback.classic.Level/ALL   :all
                        ch.qos.logback.classic.Level/TRACE :trace
                        ch.qos.logback.classic.Level/DEBUG :debug
                        ch.qos.logback.classic.Level/INFO  :info
                        ch.qos.logback.classic.Level/WARN  :warn
                        ch.qos.logback.classic.Level/ERROR :error
                        ch.qos.logback.classic.Level/OFF :off})

(def log4j2-level-map {Level/ALL   :all
                       Level/TRACE :trace
                       Level/DEBUG :debug
                       Level/INFO  :info
                       Level/WARN  :warn
                       Level/ERROR :error
                       Level/FATAL :fatal})

(defn- hostname []
  (try
    (.getCanonicalHostName (InetAddress/getLocalHost))
    (catch UnknownHostException uhe
      "UnknownHost")))

(defn- init-class []
  (-> (Thread/currentThread)
      (.getStackTrace)
      last
      (.getClassName)))

(defn- pid []
  (-> (ManagementFactory/getRuntimeMXBean)
      (.getName)
      (.split "@")
      first))

(def ^:private default-app-id
  (delay (str (hostname) "-" (init-class) "-" (pid) )))

(def ^:private default-source-id 
  (delay (-> (java.util.UUID/randomUUID) (.toString))))


(defn- -init [^IPersistentMap conf]
  (let [sourceId (or (:sourceId conf) @default-source-id)
        appId (or (:appId conf) @default-app-id)
        updated-conf (assoc conf :sourceId sourceId :appId appId)]
    (when-not (clojure.string/blank? (:url updated-conf))
      (cli/init! updated-conf))
    [[] (atom updated-conf)]))



(defn- log->samsara-event [{:keys [sourceId appId] :as conf}
                           {:keys [timestamp eventName] :as log}]
  (let [timestamp (or timestamp (System/currentTimeMillis))
        eventName (or eventName "UnknownLogEvent")
        optional-confs (select-keys conf [:serviceName])]

    (->  (assoc log
                :timestamp timestamp
                :eventName eventName
                :sourceId sourceId
                :appId appId)
         (merge optional-confs))))


(defn- send-log [conf log]
  (when-not (clojure.string/blank? (:url conf))
    (let [samsara-event (log->samsara-event conf log)]
      (try
        (cli/record-event! samsara-event)
        (catch Exception e
          (println "Unable to record event ->" samsara-event "\n Cause->" e))))))


(defn -logToConsole [this ^Boolean b]
  (swap! (.state this) assoc :log-to-console b))



(defn -log4j2Event [this ^Level log-level ^String msg ^Throwable t]
  (send-log @(.state this) {:eventName "log"
                           :loggingFramework "Log4j2"
                           :level (get log4j2-level-map log-level)
                           :message msg
                           :throwable t}))


(defn -slf4jEvent [this ^Integer log-level ^String msg ^Throwable t]
  (send-log @(.state this) {:eventName "log"
                           :loggingFramework "SLF4J"
                           :level (get slf4j-level-map log-level)
                           :message msg
                           :throwable t}))


(defn -logbackEvent [this ^ch.qos.logback.classic.Level log-level ^String msg ^IThrowableProxy t]
  (send-log @(.state this) {:eventName "log"
                            :loggingFramework "Logback"
                            :level (get logback-level-map log-level)
                            :message msg
                            :throwable t}))
