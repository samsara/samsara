(ns samsara.slf4j.SamsaraLogger 
  (:import [org.slf4j.helpers FormattingTuple MessageFormatter])
  (:gen-class :extends org.slf4j.helpers.MarkerIgnoringBase
              :constructors {[String] []}
              :state state
              :init init))

(def levels (zipmap [:trace :debug :info :warn :error] [0 1 2 3 4]))

(defn- -init[^String name]
  [[] (atom {:name name
             :current-log-level :info})])

(defn- -log [level ^String msg ^Throwable t]
  (println "**SAMSARA**" level msg t))

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
      (-log level (.getMessage tp) (.getThrowable tp)))))


(defn -isTraceEnabled [this]
  (is-level-enabled this :trace))

(defn -trace
  ([this ^String msg]
   (-log :trace msg nil))

  ([this ^String msg ^Throwable t]
   (-log :trace msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log this :trace format (conj params p))))


(defn -isDebugEnabled [this]
  (is-level-enabled this :debug))

(defn -debug
  ([this ^String msg]
   (-log :debug msg nil))

  ([this ^String msg ^Throwable t]
   (-log :debug msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log this :debug format (conj params p))))


(defn -isInfoEnabled [this]
  (is-level-enabled this :info))

(defn -info
  ([this ^String msg]
   (-log :info msg nil))

  ([this ^String msg ^Throwable t]
   (-log :info msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :info format (conj params p))))


(defn -isWarnEnabled [this]
  (is-level-enabled this :warn))

(defn -warn
  ([this ^String msg]
   (-log :warn msg nil))

  ([this ^String msg ^Throwable t]
   (-log :warn msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :warn format (conj params p))))


(defn -isErrorEnabled [this]
  (is-level-enabled this :error))

(defn -error
  ([this ^String msg]
   (-log :error msg nil))

  ([this ^String msg ^Throwable t]
   (-log :error msg t))

  ([this ^String format ^Object p & params]
   (-format-log this :error format (conj params p))))

