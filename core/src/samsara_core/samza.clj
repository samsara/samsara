(ns samsara-core.samza
  (:require [samsara-core.core :as core])
  (:require [samsara.utils :refer [to-json from-json]])
  (:import [org.apache.samza.job JobRunner]
           [org.apache.samza.config MapConfig]))

;; runtime pipeline initialized by init-pipeline!
(def internal-pipeline nil)
(def ^:dynamic *config* nil)


(defn samza-config [{:keys [job-name input-topic output-topic zookeepers brokers offset]}]
  {"job.factory.class" "org.apache.samza.job.local.ThreadJobFactory"
   "job.name"          job-name
   "task.class"        "samsara.SamsaraSystem"
   "task.inputs"       (str "kafka." input-topic)
   "serializers.registry.string.class" "org.apache.samza.serializers.StringSerdeFactory"
   "systems.kafka.samza.factory" "org.apache.samza.system.kafka.KafkaSystemFactory"
   "systems.kafka.samza.key.serde" "string"
   "systems.kafka.samza.msg.serde" "string"
   "systems.kafka.consumer.zookeeper.connect" zookeepers
   "systems.kafka.consumer.auto.offset.reset" (name offset)
   "systems.kafka.producer.bootstrap.servers" brokers
   })



(defn process-kafka-event [event]
  (->> event
       from-json
       vector
       internal-pipeline
       (map (juxt :sourceId to-json))))



(defn pipeline
  "Takes an event in json format and returns a list of tuples
  with the partition key and the event in json format
  f(x) -> List<[part-key, json-event]>"
  [^String event]
  (process-kafka-event event))



(defn output-topic! []
  (-> *config* :topics :output-topic))



(defn init-pipeline! [{:keys [index] :as config}]
  (alter-var-root #'internal-pipeline (constantly (core/samsara-processor index)))
  (alter-var-root #'*config* (constantly config)))


(defn start! [{:keys [topics] :as config}]
  (.run (JobRunner. (MapConfig. (samza-config topics)))))
