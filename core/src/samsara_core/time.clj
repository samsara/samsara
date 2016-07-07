(ns samsara-core.time
  (:require [moebius.core :refer :all]))



(defenrich is-timestamp-reliable
  #_"It verifies if the client timestamp seems reliable,
   and if it is it will inject and attribute `timeReliable`
   true/false."
  [{:keys [timestamp receivedAt publishedAt] :as event}]
  (when (and timestamp receivedAt publishedAt)
        (assoc event :timeReliable
               (cond
                ;; is the timestamp coming from
                ;; the future
                (> timestamp publishedAt)  false
                (> publishedAt receivedAt) false
                ;; is received - published within
                ;; a reasonable time (10s) otherwise we consider ti bogus
                (not (>= 10000 (Math/abs (- receivedAt publishedAt)) 0)) false
                ;; is the event generated within a
                ;; reasonable time? 10d otherwise is bogus
                (not (>= (* 10 24 60 60 1000) (- publishedAt timestamp) 0)) false
                :else true))))
