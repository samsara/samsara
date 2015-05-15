(ns org.slf4j.impl.StaticLoggerBinder
  (:import [org.slf4j.impl SamsaraLoggerFactory]
           [org.slf4j ILoggerFactory]
           [org.slf4j.spi LoggerFactoryBinder])
  (:gen-class :implements LoggerFactoryBinder
              :constructors {[] []}
              :init object-state
              :methods [^:static [getSingleton [] StaticLoggerBinder]
                        ^:static [getLoggerFactory [] ILoggerFactory]
                        ^:static [getLoggerFactoryClassStr [] String]]))

(def ^:private singleton (StaticLoggerBinder.))

(def REQUESTED_API_VERSION "1.7.12")

(defn- -init []
  [[] (atom {:loggerFactory (SamsaraLoggerFactory. )})])

(defn- -getSingleton []
  singleton)

(defn- -getLoggerFactory []
  (:loggerFactory @object-state))

(defn- -getLoggerFactoryClassStr []
  (-> (:loggerFactory @object-state) (.class) (.getName)))
 
