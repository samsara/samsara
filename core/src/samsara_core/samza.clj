(ns samsara-core.samza
  (:require [clojure.string :as str]
            [samsara-core.kernel :as kern]
            [where.core :refer [where]])
  (:import org.apache.samza.config.MapConfig
           org.apache.samza.job.JobRunner
           [org.apache.samza.system IncomingMessageEnvelope
            OutgoingMessageEnvelope SystemStream]
           [org.apache.samza.task MessageCollector TaskCoordinator]))



(defn samza-config [{:keys [job-name zookeepers brokers offset samza-overrides]}
                    streams]
  (let [primary-topics (map :input-topic (filter (where :state = :partitioned) streams))
        state-topics (concat
                      (map :state-topic (filter (where :state = :partitioned) streams))
                      (map :input-topic (filter :bootstrap streams) streams))

        inputs (->> (concat primary-topics state-topics)
                    (map (partial str "kafka."))
                    (str/join ","))

        config-overrides (into {} (map (fn [[k v]] [(str (name k)) (str v)]) samza-overrides))

        topics-config
        (into {}
              (concat
               (map
                (fn [topic]
                  [(str "systems.kafka.streams." topic ".samza.offset.default") "oldest"])
                primary-topics)

               (mapcat
                (fn [topic]
                  [[(str "systems.kafka.streams." topic ".samza.bootstrap")    "true"]
                   [(str "systems.kafka.streams." topic ".samza.reset.offset") "true"]
                   [(str "systems.kafka.streams." topic ".samza.offset.default") "oldest"]])
                state-topics)))


        config
        {"job.factory.class" "org.apache.samza.job.local.ThreadJobFactory"
         "job.name"          job-name
         "task.class"        "samsara.SamsaraSystem"
         "task.inputs"       inputs
         "task.checkpoint.factory" "org.apache.samza.checkpoint.kafka.KafkaCheckpointManagerFactory"
         "task.checkpoint.system" "kafka"
         "task.commit.ms"         "60000"

         "serializers.registry.string.class" "org.apache.samza.serializers.StringSerdeFactory"
         "systems.kafka.samza.factory" "org.apache.samza.system.kafka.KafkaSystemFactory"
         "systems.kafka.samza.key.serde" "string"
         "systems.kafka.samza.msg.serde" "string"
         "systems.kafka.consumer.zookeeper.connect" zookeepers
         "systems.kafka.consumer.auto.offset.reset" (name offset)
         "systems.kafka.producer.bootstrap.servers" brokers
         }]
    (merge config topics-config config-overrides)))



(defn display-samza-config [samza-config]
  (println "\t------------------------------ Samza Config ------------------------------")
  (doseq [[k v] (sort-by first samza-config)]
    (println "\t" k " = " v ))
  (println "\t--------------------------------------------------------------------------"))


(def topic->stream
  "Given a topic it return the equivalent Samza' SystemStream"
  (memoize (fn [topic] (SystemStream. "kafka" topic))))


(defn- output-collector [collector]
  (fn [all-output]
    (doseq [[stream key message] all-output]
      ;; TODO: remove this and remove the INPUT: one as well
      (kern/printf-stream "OUTPUT[%s//%s] : %s\n" stream key message)
      (.send collector (OutgoingMessageEnvelope.
                        (topic->stream stream) key message )))))


(defn samza-process
  "Takes an event as a JSON encoded String and depending on the stream
   name dispatches the function to the appropriate handler.
   One handler is the normal pipeline processing, the second handler
   is for the kv-store"
  [^IncomingMessageEnvelope envelope,
   ^MessageCollector collector,
   ^TaskCoordinator coordinator
   ^String  stream
   ^Integer partition
   ^String  key
   ^String  message]

  (kern/process-dispatch
   (output-collector collector)
   stream partition key message))


(defn start! [{:keys [streams job] :as config}]
  (let [cfg (samza-config job streams)]
    (display-samza-config cfg)
    (.run (JobRunner. (MapConfig. cfg)))))
