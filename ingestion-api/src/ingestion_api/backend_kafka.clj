(ns ingestion-api.backend-kafka
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend :refer [send]])
  (:import  [ingestion_api.backend EventsQueueingBackend])
  (:require [clj-kafka.producer :as kp])
  (:require [schema.core :as s])
  (:require [cheshire.core :as json]))


(def default-producer-config
  "Default configuration for the Kafka producer.
  By default the events will be sent asynchronously and
  the producer will wait for all brokers to acknowledge
  the events."

  {"serializer.class"         "kafka.serializer.StringEncoder"
   "partitioner.class"        "kafka.producer.DefaultPartitioner"
   "request.required.acks"    "-1"
   "producer.type"            "async"
   "message.send.max.retries" "5" })

;;
;;  Kafka backend sends the events into a Kafka topic as single
;;  json lines.
;;
(deftype KafkaBackend [conf topic producer]
  EventsQueueingBackend

  (send [_ events]
    (->> events
         (map (juxt (constantly topic) :sourceId  #(json/generate-string % {:pretty false})))
         (map (fn [[topic key message]] (kp/message topic key message)))
         (kp/send-messages producer))))


(defn- check-config
  "Check the validity of a Kafka configuration"
  [config]
  (s/validate
   {(s/required-key "topic") s/Str
    (s/required-key "metadata.broker.list") s/Str
    s/Str s/Str}
   config))


(defn make-kafka-backend
  "Create a kafka backend"
  [config]
  (let [{:strs [topic] :as cfg} (->> config
                                     (map (fn [[k v]] [(name k) (str v)]))
                                     (into {})
                                     (merge default-producer-config))

        ;; check config
        _ (check-config cfg)

        ;; connect to brokers
        producer (kp/producer cfg)]

    (KafkaBackend. cfg topic producer)))
