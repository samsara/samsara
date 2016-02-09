(ns samsara.logger.EventLoggerBuilder
  (:require [samsara.client :as cli])
  (:import [samsara.logger EventLogger])

  (:gen-class :constructors {[][]}
              :state state
              :init init
              :methods [[setApiUrl [String] Object]
                        [getApiUrl [] String]
                        [setAppId  [String] Object]
                        [getAppId  [] String]
                        [setSourceId [String] Object]
                        [getSourceId [] String]
                        [setPublishInterval [Long] Object]
                        [getPublishInterval [] Long]
                        [setMinBufferSize [Long] Object]
                        [getMinBufferSize [] Long]
                        [setMaxBufferSize [Long] Object]
                        [getMaxBufferSize [] Long]
                        [setCompression [String] Object]
                        [getCompression [] String]
                        [setServiceName [String] Object]
                        [getServiceName [] String]
                        [build [] samsara.logger.EventLogger]
                        [sendToSamsara [] Boolean]]))

(def ^:private default-overrides {:publish-interval 5000
                                  :min-buffer-size 1
                                  :max-buffer-size 10000
                                  :compression :gzip})

(def ^:private known-compression-types #{:none :gzip})

(def ^:private default-conf (merge cli/DEFAULT-CONFIG default-overrides))

(defn- str->compress-type [s]
  (-> s
      clojure.string/lower-case
      keyword
      known-compression-types))

(defn- -init []
  [[] (atom default-conf)])

(defn -setApiUrl [this ^String url]
  (swap! (.state this) assoc :url url)
  this)

(defn -getApiUrl [this]
  (:url @(.state this)))

(defn -setAppId [this ^String appId]
  (swap! (.state this) assoc :appId appId)
  this)

(defn -getAppId [this]
  (:appId @(.state this)))

(defn -setSourceId [this ^String sourceId]
  (swap! (.state this) assoc :sourceId sourceId)
  this)

(defn -getSourceId [this]
  (:sourceId @(.state this)))

(defn -setPublishInterval [this ^Long interval]
  (swap! (.state this) assoc :publish-interval interval)
  this)

(defn -getPublishInterval [this]
  (:publish-interval @(.state this)))

(defn -setMinBufferSize [this ^Long min-size]
  (swap! (.state this) assoc :min-buffer-size min-size)
  this)

(defn -getMinBufferSize [this]
  (:min-buffer-size @(.state this)))

(defn -setMaxBufferSize [this ^Long max-size]
  (swap! (.state this) assoc :max-buffer-size max-size)
  this)

(defn -getMaxBufferSize [this]
  (:max-buffer-size @(.state this)))

(defn -setCompression[this ^String s]
  (if-let [compress-type (str->compress-type s)]
    (swap! (.state this) assoc :compression compress-type)
    (binding [*out* *err*]
      (println "WARNING! Unknown compression type [" s
               "] Defaulting compression type to ["
               (name (:compression @(.state this)))
               "]")))
  this)

(defn -getCompression [this]
  (:compression @(.state this)))

(defn -setServiceName [this ^String s]
  (swap! (.state this) assoc :serviceName s)
  this)

(defn -getServiceName [this]
  (:serviceName @(.state this)))

(defn -build [this]
  (EventLogger. @(.state this)))

(defn -sendToSamsara [this]
  (if (clojure.string/blank? (:url @(.state this)))
    false
    true))
