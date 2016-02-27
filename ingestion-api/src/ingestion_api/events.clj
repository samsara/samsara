(ns ingestion-api.events
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend.backend-protocol :refer [send]])
  (:require [samsara.trackit :refer [track-time]]))

(defn send!
  [backend events]
  (track-time "ingestion.events.backend-send"
              (send backend events)))
