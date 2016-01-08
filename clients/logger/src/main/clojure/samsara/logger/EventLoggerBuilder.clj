(ns samsara.logger.EventLoggerBuilder
  (:require [samsara.client :as cli])
  (:import [samsara.logger EventLogger])

  (:gen-class :constructors {[][]}
              :state state
              :init init
              :methods [[setApiUrl [String] Object]
                        [getApiUrl [] String]
                        [setSourceId [String] Object]
                        [getSourceId [] String]
                        [setPublishInterval [Integer] Object]
                        [getPublishInterval [] Integer]
                        [build [] samsara.logger.EventLogger]
                        [sendToSamsara [] Boolean]]))

(def ^:private default-conf cli/DEFAULT-CONFIG)

(defn- -init []
  [[] (atom default-conf)])

(defn -setApiUrl [this ^String url]
  (swap! (.state this) assoc :url url)
  this)

(defn -getApiUrl [this]
  (:url @(.state this)))

(defn -setSourceId [this ^String sourceId]
  (swap! (.state this) assoc :sourceId sourceId)
  this)

(defn -getSourceId [this]
  (:sourceId @(.state this)))

(defn -setPublishInterval [this ^Integer interval]
  (swap! (.state this) assoc :publish-interval interval)
  this)

(defn -getPublishInterval [this]
  (:publish-interval @(.state this)))

(defn -build [this]
  (EventLogger. @(.state this)))



(defn -sendToSamsara [this]
  (if (clojure.string/blank? (:url @(.state this)))
    false
    true))
