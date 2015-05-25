(ns samsara-core.samza
  (:require [samsara-core.core :refer [process-raw-event]]))

(defn pipeline [^String event]
  (println "xxxxxxxxxxxxxxxxxxxx:" event)
  (process-raw-event event))
