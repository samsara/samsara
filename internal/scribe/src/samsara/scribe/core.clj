(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json])
  (:require [samsara.scribe.serializer.edn :as edn])
  (:require [samsara.scribe.serializer.nippy :as nippy]))


(defmulti scribe :type)


(defmethod scribe :json
  [config]
  (json/make-json-scribe config))

(defmethod scribe :edn
  [config]
  (edn/make-edn-scribe config))

(defmethod scribe :nippy
  [config]
  (nippy/make-nippy-scribe config))
