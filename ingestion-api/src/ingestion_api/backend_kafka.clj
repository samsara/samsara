(ns ingestion-api.backend-kafka
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend :refer [send]])
  (:import  [ingestion_api.backend EventsQueueingBackend])
  (:require [clj-kafka.producer :as kp])
  (:require [schema.core :as s])
  (:require [cheshire.core :as json]))


(def default-producer-config
  {"serializer.class"         "kafka.serializer.StringEncoder"
   "partitioner.class"        "kafka.producer.DefaultPartitioner"
   "request.required.acks"    "-1"
   "producer.type"            "async"
   "message.send.max.retries" "5" })


(deftype KafkaBackend [conf topic producer]
  EventsQueueingBackend

  (send [_ events]
    (->> events
         (map (juxt (constantly topic) :sourceId  #(json/generate-string % {:pretty false})))
         (map (fn [[topic key message]] (kp/message topic key message)))
         (kp/send-messages producer))))


(defn- check-config [config]
  (s/validate
   {(s/required-key "topic") s/Str
    (s/required-key "metadata.broker.list") s/Str
    s/Str s/Str}
   config))


(defn make-kafka-backend [config]
  (let [cfg (->> config
                 (map (fn [[k v]] [(name k) (str v)]))
                 (into {})
                 (merge default-producer-config))

        ;; check config
        _ (check-config cfg)

        ;; connect to brokers
        producer (kp/producer cfg)]

    (KafkaBackend. cfg (cfg "topic") producer)))
