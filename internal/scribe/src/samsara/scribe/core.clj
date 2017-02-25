(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json]
            [samsara.scribe.serializer.edn :as edn]
            [samsara.scribe.serializer.nippy :as nippy]
            [samsara.scribe.serializer.fressian :as fressian]
            [samsara.scribe.serializer.transit :as transit]))


(defmulti scribe :type)


(defmethod scribe :json
  [config]
  (json/make-json-scribe (dissoc config :type)))

(defmethod scribe :edn
  [config]
  (edn/make-edn-scribe (dissoc config :type)))

(defmethod scribe :nippy
  [_]
  (nippy/make-nippy-scribe))

(defmethod scribe :fressian
  [_]
  (fressian/make-fressian-scribe))

(defmethod scribe :transit
  [config]
  (transit/make-transit-scribe (dissoc config :type)))
