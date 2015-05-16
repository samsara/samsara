(ns samsara.slf4j.LoggerFactory
  (:import [samsara.slf4j Logger]
           [org.slf4j Logger])
  (:gen-class :implements [org.slf4j.ILoggerFactory]
              :constructors {[] []}
              :state state
              :init init))


(defn- -init []
  [[] (atom {:logger-map {}})]) ; initialise logger-map with an atom

(defn- -getLogger ^Logger[this ^String name]
  (locking this
    (if-let [logger (get-in @(.state this) [:logger-map name])]
      logger
      (let [logger (Logger. name)]
        (swap! (.state this) assoc-in [:logger-map  name] logger)
        logger))))

