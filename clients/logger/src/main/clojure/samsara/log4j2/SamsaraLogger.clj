(ns samsara.log4j2.SamsaraLogger
  (:require [samsara.logger.core :refer [send-event]])
  (:import [org.apache.logging.log4j Level])
  (:gen-class :constructors {[String String] []}
              :state state
              :init init
              :methods [[logEvent [org.apache.logging.log4j.Level String Throwable] void]]))

(def level<->keyword (zipmap [Level/ALL Level/TRACE Level/DEBUG Level/INFO Level/WARN Level/ERROR Level/FATAL Level/OFF]
                             [:all :trace :debug :info :warn :error :fatal :off]))

(defn- warn-message []
  (println "****************************************************************")
  (println "SAMSARA: The environment variable \"SAMSARA_API_URL\" is not set")
  (println "SAMSARA: Samsara Log4j2 logger will just print to console")
  (println "****************************************************************\n"))


(defn- -init [^String api-url ^String source-id]
  (when (empty? api-url)
    (warn-message))
  [[] {:url api-url
       :sourceId source-id}])


(defn- -logEvent [this ^Level level ^String msg ^Throwable t]
  (let [conf (.state this)]
    (send-event conf {:eventName "log"
                      :loggingFramework "Log4j2"
                      :level (level<->keyword level)
                      :message msg
                      :throwable t})))
