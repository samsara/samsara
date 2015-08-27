(ns samsara.logger.EventLogger
  (:require [samsara.client :as cli])
  (:import [java.net InetAddress UnknownHostException]
           [java.lang.management ManagementFactory]
           [org.slf4j.spi LocationAwareLogger]
           [org.apache.logging.log4j Level]
           )
  (:gen-class 
              :constructors {[String String] []
                             [String String Integer] []}
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


(defn- log->samsara-event [{:keys [sourceId appId] :as conf}

                           {:keys [timestamp eventName] :as log}]

  (let [sourceId (or sourceId default-app-id)
        appId (or appId default-app-id)
        timestamp (or timestamp (System/currentTimeMillis))
        eventName (or eventName "UnknownLogEvent")]

    (assoc log
           :timestamp timestamp
           :eventName eventName
           :sourceId sourceId
           :appId appId)))


(defn- -init
  ([^String api-url ^String source-id]
              (let [conf {:url api-url
                          :sourceId source-id}]

                (-init conf)))

  ([^String api-url ^String source-id ^Integer publish-interval]
   (let [conf {:url api-url
               :sourceId source-id
               :publish-interval publish-interval}]

     (-init conf)))

  ([conf]
   (when-not (empty? (:url conf))
     (cli/init! conf))
   [[] (atom conf)]))



(defn- send-log [conf log]
  (if (empty? (:url conf))
    (let [samsara-event (log->samsara-event conf log)]
      (try
        (cli/record-event samsara-event)
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
