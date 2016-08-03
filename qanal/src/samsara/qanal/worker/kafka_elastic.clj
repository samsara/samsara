(ns samsara.qanal.worker.kafka-elastic
  (:require [kafkian.consumer :as kc]
            [samsara.qanal.worker.worker-protocol :refer [Worker]]
            [taoensso.timbre :as log]))

(def ^:const DEFAULT-STATE {:jobs nil
                            :running false})

(deftype KafkaElasticWorker [id kafka-brokers topics mutable-state]
  Worker

  (all-jobs [this]
    (let [conf {"bootstrap.servers" kafka-brokers
                "group.id" (str "qanal-" id)}
          c (kc/consumer conf (kc/string-deserializer) (kc/string-deserializer))
          partitions (mapcat #(kc/list-all-partitions c %) topics)
          jobs (mapv (fn [m] {:topic (:topic m) :partition (:partition m)}) partitions)]
      (.close c)
      jobs))

  (assigned-jobs [this]
    (:jobs @mutable-state))

  (start [this jobs]
    (locking mutable-state
      (when-not (:running @mutable-state)
        (swap! mutable-state assoc :jobs jobs)
        (swap! mutable-state assoc :running true)
        (log/info "Started working on " (:jobs @mutable-state)))))

  (stop [this]
    (locking mutable-state
      (when (:running @mutable-state)
        (swap! mutable-state assoc :running false )
        (log/info "Stopped working on " (:jobs @mutable-state))))))



(defn create-kafka-elastic-worker [id kafka-brokers topics]
  (->KafkaElasticWorker id kafka-brokers topics (atom DEFAULT-STATE)))
