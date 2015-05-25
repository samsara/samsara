(ns samsara-core.main
  (:require [clojure.string :as s])
  (:import [org.apache.samza.job JobRunner]
           [org.apache.samza.config MapConfig])
  (:gen-class))


  (def config "
job.factory.class=org.apache.samza.job.local.ThreadJobFactory
job.name=Samsara

# Task
task.class=samsara.SamsaraSystem

task.inputs=kafka.events

# Serializers
serializers.registry.string.class=org.apache.samza.serializers.StringSerdeFactory

# Systems
systems.kafka.samza.factory=org.apache.samza.system.kafka.KafkaSystemFactory
systems.kafka.samza.msg.serde=string
systems.kafka.consumer.zookeeper.connect=localhost:2181/
systems.kafka.consumer.auto.offset.reset=smallest
systems.kafka.producer.bootstrap.servers=localhost:9092
")



(defn -main [& args]
  (println "(*) Starting Samsara's Analytics Pipeline")
  (let [cfg (->> config s/split-lines (map s/trim) (filter (complement #(.startsWith % "#"))) (filter #(not= % "")) (map #(s/split % #"=")) (into {}))]
    (.run (JobRunner. (MapConfig. cfg)))))
