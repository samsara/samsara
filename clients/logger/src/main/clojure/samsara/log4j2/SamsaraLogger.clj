(ns samsara.log4j2.SamsaraLogger
  (:require [samsara.logger.core :refer [send-event]])
  (:import [org.apache.logging.log4j Level])
  (:gen-class :constructors {[String] []}
              :state state
              :init init
              :methods [[logEvent [org.apache.logging.log4j.Level String Throwable] void]]))

(def level<->keyword (zipmap [Level/ALL Level/TRACE Level/DEBUG Level/INFO Level/WARN Level/ERROR Level/FATAL Level/OFF]
                             [:all :trace :debug :info :warn :error :fatal :off]))

(defn- -init [^String api-url]
  [[] {:api-url api-url}])


(defn- -logEvent [this ^Level level ^String msg ^Throwable t]
  (send-event {:eventName "log"
               :loggingFramework "Log4j2"
               :level (level<->keyword level)
               :message msg
               :throwable t}))
