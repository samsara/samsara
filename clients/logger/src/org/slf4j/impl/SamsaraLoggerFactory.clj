(ns org.slf4j.impl.SamsaraLoggerFactory
  (:import [org.slf4j.impl SamsaraLogger]
           [org.slf4j Logger ILoggerFactory])
  (:gen-class :implements ILoggerFactory
              :constructors {[] []}
              :state object-state
              :init init))


(defn- -init []
  [[] (atom {:logger-map {}})]) ; initialise logger-map with an atom

(defn- -getLogger ^Logger[this ^String name]
  (locking this
    (if-let [logger (get-in @(.object-state this) [:logger-map name])]
      logger
      (let [logger (SamsaraLogger. name)]
        (swap! @(.object-state this) assoc-in [:logger-map  name] logger)
        logger))))

