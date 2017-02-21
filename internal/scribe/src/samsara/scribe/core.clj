(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json]
            [samsara.scribe.serializer.edn :as edn]
            [samsara.scribe.serializer.nippy :as nippy]
            [samsara.scribe.serializer.fressian :as fressian]))


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

(defmethod scribe :fressian
  [config]
  (fressian/make-fressian-scribe config))
