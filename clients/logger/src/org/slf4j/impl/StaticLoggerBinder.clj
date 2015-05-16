(ns org.slf4j.impl.StaticLoggerBinder
  ;;(:import [org.slf4j.impl SamsaraLoggerFactory])
  (:gen-class :implements [org.slf4j.spi.LoggerFactoryBinder]
              ;;:constructors {[] []}
              ;;:init state
              :methods [^:static [getSingleton [] Object]
                        [getLoggerFactory [] org.slf4j.ILoggerFactory]
                        [getLoggerFactoryClassStr [] String]
                        ]))

(def REQUESTED_API_VERSION "1.7.12")

(defn- singleton [] 
  ;;bit of a hack this
  (memoize (->  "org.slf4j.impl.SamsaraLoggerBinder"
                Class/forName
                (.newInstance))))

#_(defn- -init []
  [[] {:loggerFactory (SamsaraLoggerFactory. )}])

(defn -getSingleton []
  (singleton))

(defn- -getLoggerFactory [this]
  #_(:loggerFactory (.state this))
  (.getLoggerFactory (singleton)))

(defn- -getLoggerFactoryClassStr [this]
  #_(-> (:loggerFactory (.state this)) (.class) (.getName))
  (.getLoggerFactoryClassStr (singleton)))
