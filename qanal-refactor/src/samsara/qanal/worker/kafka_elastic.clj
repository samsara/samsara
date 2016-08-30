(ns samsara.qanal.worker.kafka-elastic
  (:require [kafkian.consumer :as kc]
            [samsara.qanal.worker.worker-protocol :refer [Worker]]
            [taoensso.timbre :as log]))

(def ^:const DEFAULT-STATE {:jobs nil
                            :running false})

(deftype KafkaElasticWorker [id kafka-brokers topics state]
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
    (:jobs @state))

  (start [this jobs]
    (swap! state
           (fn [{:keys [running] :as s}]
             (if running
               s
               (do
                 (log/info "Started working on " jobs)
                 (assoc s :jobs jobs :running true))))))

  (stop [this]
    (swap! state
           (fn [{:keys [running jobs] :as s}]
             (if running
               (do
                 (log/info "Stopped working on " jobs)
                 (assoc s :running false))
               s)))))



(defn create-kafka-elastic-worker [id kafka-brokers topics]
  (->KafkaElasticWorker id kafka-brokers topics (atom DEFAULT-STATE)))
