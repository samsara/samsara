(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json]))


(defmulti scribe :type)


(defmethod scribe :json
  [config]
  (json/make-json-scribe config))
