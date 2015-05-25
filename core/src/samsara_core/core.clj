(ns samsara-core.core
  (:require [samsara.utils :refer [to-json from-json]]))

(defn process-raw-event [event]
  (-> event
      from-json
      (assoc :ciao "bruno")
      to-json))
