(ns samsara.slf4j.SamsaraLogger 
  (:require [samsara.logger.core :refer [send-event]])
  (:import [org.slf4j.helpers FormattingTuple MessageFormatter]
           [java.net InetAddress UnknownHostException])
  (:gen-class :extends org.slf4j.helpers.MarkerIgnoringBase
              :constructors {[String] []}
              :state state
              :init init))

(def levels (zipmap [:trace :debug :info :warn :error] [0 1 2 3 4]))

(def ^:private default-sourceId (str "SLF4J-"
                                     (try
                                       (.getCanonicalHostName (InetAddress/getLocalHost))
                                       (catch UnknownHostException uhe
                                         "UnknownHostException"))))

(defn- generate-config []
  {:url (System/getenv "SAMSARA_API_URL")
   :sourceId (or (System/getenv "SAMSARA_SOURCE_ID") default-sourceId)}
  )

(defn- warn-message []
  (println "****************************************************************")
  (println "SAMSARA: The environment variable \"SAMSARA_API_URL\" is not set")
  (println "SAMSARA: Samsara SLF4J logger will just print to console")
  (println "****************************************************************\n"))

(defn- -init[^String name]
  (let [conf (generate-config)]
    (when (nil? (:url conf))
      (warn-message))
    [[] (atom {:name name
               :current-log-level :info
               :samsara-conf conf})]))

(defn- -log [this level ^String msg ^Throwable t]
  (let [conf (:samsara-conf @(.state this))]
    (send-event conf {:eventName "log"
                      :loggingFramework "SLF4J"
                      :level level
                      :message msg
                      :throwable t})))

(defn- level-value [level]
  (level levels))

(defn- is-level-enabled [this level]
  (let [lv (level-value level)
        clv (-> (:current-log-level @(.state this))
                level-value)]
    (>= lv clv)))

(defn- -format-log [this level ^String format args]
  (when (is-level-enabled this level)
    (let [^FormattingTuple tp (MessageFormatter/arrayFormat format (into-array Object args))]
      (-log this level (.getMessage tp) (.getThrowable tp)))))


(defn -isTraceEnabled [this]
  (is-level-enabled this :trace))

(defn -trace
  ([this ^String msg]
   (-log this :trace msg nil))

  ([this ^String msg ^Throwable t]
   (-log this :trace msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log this :trace format (conj params p))))


(defn -isDebugEnabled [this]
  (is-level-enabled this :debug))

(defn -debug
  ([this ^String msg]
   (-log this :debug msg nil))

  ([this ^String msg ^Throwable t]
   (-log this :debug msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log this :debug format (conj params p))))


(defn -isInfoEnabled [this]
  (is-level-enabled this :info))

(defn -info
  ([this ^String msg]
   (-log this :info msg nil))

  ([this ^String msg ^Throwable t]
   (-log this :info msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :info format (conj params p))))


(defn -isWarnEnabled [this]
  (is-level-enabled this :warn))

(defn -warn
  ([this ^String msg]
   (-log this :warn msg nil))

  ([this ^String msg ^Throwable t]
   (-log this :warn msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :warn format (conj params p))))


(defn -isErrorEnabled [this]
  (is-level-enabled this :error))

(defn -error
  ([this ^String msg]
   (-log this :error msg nil))

  ([this ^String msg ^Throwable t]
   (-log this :error msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :error format (conj params p))))

