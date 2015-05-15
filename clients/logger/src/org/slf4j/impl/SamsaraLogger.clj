(ns org.slf4j.impl.SamsaraLogger
  (:import [org.slf4j.helpers MarkerIgnoringBase FormattingTuple MessageFormatter])
  (:gen-class :extends MarkerIgnoringBase
              :constructors {[String] []}
              :state object-state
              :init init))

(def levels (zipmap [:trace :debug :info :warn :error] [0 1 2 3 4]))

(defn- -init[^String name]
  [[] (atom {:name name
             :current-log-level :info})])

(defn- -log [level ^String msg ^Throwable t]
  (println level msg t))

(defn- level-value [level]
  (level levels))

(defn- is-level-enabled [this level]
  (let [lv (level-value level)
        clv (-> (:current-log-level @(.object-state this))
                level-value)]
    (>= lv clv)))

(defn- -format-log [level ^String format args]
  (when (is-level-enabled level)
    (let [^FormattingTuple tp (MessageFormatter/arrayFormat format (into-array Object args))]
      (-log level (.getMessage tp) (.getThrowable tp)))))


(defn -isTraceEnabled [this]
  (is-level-enabled :trace))

(defn -trace
  ([this ^String msg]
   (-log :trace msg nil))

  ([this ^String msg ^Throwable t]
   (-log :trace msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log :trace format (conj params p))))


(defn -isDebugEnabled [this]
  (is-level-enabled :debug))

(defn -debug
  ([this ^String msg]
   (-log :debug msg nil))

  ([this ^String msg ^Throwable t]
   (-log :debug msg t))
  
  ([this ^String format ^Object p & params]
   (-format-log :debug format (conj params p))))


(defn -isInfoEnabled [this]
  (is-level-enabled :info))

(defn -info
  ([this ^String msg]
   (-log :info msg nil))

  ([this ^String msg ^Throwable t]
   (-log :info msg t))

  ([this ^String format ^Object p & params]
   (-format-log :info format (conj params p))))


(defn -isWarnEnabled [this]
  (is-level-enabled :warn))

(defn -warn
  ([this ^String msg]
   (-log :warn msg nil))

  ([this ^String msg ^Throwable t]
   (-log :warn msg t))

  ([this ^String format ^Object p & params]
   (-format-log :warn format (conj params p))))


(defn -isErrorEnabled [this]
  (is-level-enabled :error))

(defn -error
  ([this ^String msg]
   (-log :error msg nil))

  ([this ^String msg ^Throwable t]
   (-log :error msg t))

  ([this ^String format ^Object p & params]
   (-format-log :error format (conj params p))))

