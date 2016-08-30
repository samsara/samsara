(ns samsara.qanal.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [samsara.qanal.coordinator.zookeeper :refer [create-zk-coordinator]]
            [samsara.qanal.worker.kafka-elastic
             :refer
             [create-kafka-elastic-worker]]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import java.util.UUID))

(defonce QANAL-ID (delay (str (UUID/randomUUID))))

(def ^:private config-schema
  {:coordinator {:type (s/enum :zookeeper)
                 :config {:zk-connect s/Str}}
   :worker {:type (s/enum :kafka-elastic)
            :config {:kafka-brokers s/Str
                     :topics [s/Str]}}})

(defn parse-opt-errors->str [errors]
  (str "There was an error in the command line : \n" (clojure.string/join \newline errors)))


(def ^:private known-options
  [
   ["-c" "--config CONFIG" "Configuration File"
    :validate [#(.exists (io/file %)) "The given file must exist"]]
   ])

(defn exit [exit-code msg]
  (println msg)
  (System/exit exit-code))

(defn read-config-file [file-name]
  (when file-name
    (println "Reading config file : " file-name)
    (edn/read-string (slurp file-name))))


(defmulti create-worker (fn [{:keys [type config]}] type))

(defmethod create-worker :kafka-elastic [{:keys [config]}]
  (create-kafka-elastic-worker @QANAL-ID (:kafka-brokers config) (:topics config)))

(defmulti create-coordinator (fn [{:keys [type config]} _] type))

(defmethod create-coordinator :zookeeper [{:keys [config]} worker]
  (create-zk-coordinator @QANAL-ID (:zk-connect config) worker))



(defn start-qanal [conf]
  (let [worker (create-worker (:worker conf))
        coordinator (create-coordinator (:coordinator conf) worker)]
    (.start coordinator)
    (log/info "Qanal Service started with ID ->" @QANAL-ID)

    ;; We can't assume/force coordinator and worker implementations to have
    ;; non-daemon threads. (JVM stops when there are no non-daemon threads running)
    ;; So we'll have a non-daemon thread here, for now I'll just have it get
    ;; the list of the worker's assigned jobs
    (future
      (while true
        (log/info "[Qanal" @QANAL-ID "] processing" (.assigned-jobs worker))
        (Thread/sleep (* 60 1000))))

    coordinator))

(defn -main [& args]
  (let [{:keys [options errors ]} (parse-opts args known-options)
        config-file (:config options)]
    (when errors
      (exit 1 (parse-opt-errors->str errors)))
    (when (nil? config-file)
      (exit 2 "Please supply a configuration file via -c option"))
    (let [cfg (read-config-file config-file)]
      (when-let [errors (s/check config-schema cfg)]
        (exit 3 (str "Please fix the configuration file: " errors)))
      (start-qanal cfg))))



(comment

  (def coord (-main "-c" "./conf/config.edn"))

  (.stop coord)

  )
