(ns org.slf4j.impl.SamsaraLoggerFactory
  (:import [org.slf4j.impl SamsaraLogger]
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
      (let [logger (SamsaraLogger. name)]
        (swap! (.state this) assoc-in [:logger-map  name] logger)
        logger))))

