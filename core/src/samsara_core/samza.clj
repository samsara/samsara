(ns samsara-core.samza
  (:require [samsara-core.core :as core])
  (:require [samsara.utils :refer [to-json from-json invariant]])
  (:require [samsara.trackit :refer [track-time count-tracker distribution-tracker]])
  (:import [org.apache.samza.job JobRunner]
           [org.apache.samza.config MapConfig]))

;; runtime pipeline initialized by init-pipeline!
(def ^:dynamic *pipeline*     nil)
(def ^:dynamic *raw-pipeline* nil)
(def ^:dynamic *config*       nil)



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



;; track
;; how many messages in / out
;; how many msg/sec
;; how big is msg in / out
;; distrin of msg size in / out
;; processing time
(defn- make-trackers [config]
  (let [topic (-> config :topics :input-topic)
        prefix (str "pipeline." topic)

        track-time-name (str prefix ".overall-processing.time")
        track-time-name2 (str prefix ".pipeline-processing.time")

        track-pipeline-time (fn [pipeline] (fn [x] (track-time track-time-name2 (pipeline x))))

        total-size-in (count-tracker (str prefix ".in.total-size"))
        dist-size-in (distribution-tracker (str prefix ".in.size"))

        total-size-out(count-tracker (str prefix ".out.total-size"))
        dist-size-out (distribution-tracker (str prefix ".out.size"))

        size-in-tracker (invariant #(let [sz (count %)]
                                   (total-size-in sz)
                                   (dist-size-in sz)))
        size-out-tracker (invariant #(let [sz (count (second %))]
                                   (total-size-out sz)
                                   (dist-size-out sz)))

        ]
    {:track-time-name track-time-name
     :size-in-tracker size-in-tracker
     :size-out-tracker size-out-tracker
     :track-pipeline-time track-pipeline-time}))



(defn make-raw-pipeline [config]
  (let [part-key-fn (-> config :topics :output-topic-partition-fn)
        {:keys [track-time-name track-pipeline-time size-in-tracker size-out-tracker]} (make-trackers config)
        pipeline (track-pipeline-time *pipeline*)]
    (fn [event]
      (track-time track-time-name
                  (->> event
                       size-in-tracker
                       from-json
                       vector
                       pipeline
                       (map (juxt part-key-fn to-json))
                       (map size-out-tracker))))))



(defn pipeline
  "Takes an event in json format and returns a list of tuples
  with the partition key and the event in json format
  f(x) -> List<[part-key, json-event]>"
  [^String event]
  (*raw-pipeline* event))



(defn output-topic! []
  (-> *config* :topics :output-topic))



(defn init-pipeline! [config]
  (alter-var-root #'*pipeline*     (constantly (core/make-samsara-processor config)))
  (alter-var-root #'*raw-pipeline* (constantly (make-raw-pipeline config)))
  (alter-var-root #'*config*       (constantly config)))


(defn start! [{:keys [topics] :as config}]
  (.run (JobRunner. (MapConfig. (samza-config topics)))))
