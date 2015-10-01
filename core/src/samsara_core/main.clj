(ns samsara-core.main
  (:require [samsara-core.samza :as samza])
  (:require [samsara.utils :refer [stoppable-thread]])
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log])
  (:require [samsara.trackit :refer [start-reporting! set-base-metrics-name!
                                     get-metric]])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl :as cider])
  (:gen-class))


(def DEFAULT-CONFIG
  {:topics
   {:job-name "Samsara"
    :input-topic "ingestion"
    :input-partitions :all
    ;; :kvstore-topic "ingestion-kv"
    :output-topic "events"
    :output-topic-partition-fn :sourceId
    ;; a CSV list of hosts and ports (and optional path)
    :zookeepers "127.0.0.1:2181"
    ;; a CSV list of host and ports of kafka brokers
    :brokers "127.0.0.1:9092"
    :offset :smallest}

   ;; metrics trackings
   :tracking
   {:enabled false :type :console}

   ;; display console progress
   :console-progress
   {:enabled true :display-every 3000}

   ;; logging
   :log
   {:timestamp-pattern "yyyy-MM-dd HH:mm:ss.SSS zzz"}

   ;; nREPL
   :nrepl
   {;; REPL server is enabled by default
    :enabled true
    :port 4555}
   })



(def cli-options
  "Command line options"
  [
   ["-c" "--config FILE" "Config file (.edn)"
    :validate [#(.exists (io/file %)) "The given file must exist"]]

   ["-h" "--help"]
   ])


(defn message
  "Prints a message in the stderr channel"
  [& m]
  (binding [*out* *err*]
    (apply println m)))


(defn version
  "Reads the version from the project.clj file
  which is typically boundled with jar file"
  []
  (try
    (->> "project.clj"
         ((fn [f] (if (.exists (io/file f)) (io/file f) (io/resource f))))
         slurp
         read-string
         nnext
         first
         (str "v"))
    (catch Exception x "")))


(defn- headline
  "Return a string with the headline and the version number"
  []
  (format
   "
-----------------------------------------------
   _____
  / ___/____ _____ ___  _________ __________ _
  \\__ \\/ __ `/ __ `__ \\/ ___/ __ `/ ___/ __ `/
 ___/ / /_/ / / / / / (__  ) /_/ / /  / /_/ /
/____/\\__,_/_/ /_/ /_/____/\\__,_/_/   \\__,_/

  Samsara CORE
-----------------------------| %7.7s |-------

" (version)))


(defn- help!
  "Pritns a help message"
  [errors]
  (let [err-msg (if-not errors "" (clojure.string/join "\n" errors))]
    (message
     (format
      "%s%s

SYNOPSIS
       ./samsara-core -c config.edn

DESCRIPTION
       Starts a processing pipeline node which will start
       consuming messages from the Ingestion-API sent to
       Kafka and produce a richer stream of event back
       into Kafka.

  OPTIONS:

  -c --config config-file.edn [REQUIRED]
       Configuration file to set up the processing node.
       example: './config/config.edn'

  -h --help
       this help page

" (headline) err-msg))))


(defn exit!
  "System exit with error state"
  [n]
  (System/exit n))


(defn- read-config
  "Read the user's configuration file
  and apply the default values."
  [config-file]
  (->> (io/file config-file)
       slurp
       read-string
       (merge-with merge DEFAULT-CONFIG)
       ((fn [cfg]
          (if (= :all (-> cfg :topics :input-partitions))
            (assoc-in  cfg [:topics :input-partitions] identity)
            (update-in cfg [:topics :input-partitions] (partial into #{})))))))


(defn- init-log!
  "Initializes log settings"
  [cfg]
  (log/set-config! [:fmt-output-fn]
                   (fn [{:keys [level throwable message timestamp hostname ns]}
                       ;; Any extra appender-specific opts:
                       & [{:keys [nofonts?] :as appender-fmt-output-opts}]]
                     ;; <timestamp> <hostname> <LEVEL> [<ns>] - <message> <throwable>
                     (format "%s %s [%s] - %s%s"
                             timestamp (-> level name clojure.string/upper-case) ns (or message "")
                             (or (log/stacktrace throwable "\n" (when nofonts? {})) ""))))
  (log/merge-config! cfg))



(defn show-console-progress!
  [config]
  (if (-> config :console-progress :enabled)
    (let [topic (-> config :topics :input-topic)
          metric (str "pipeline." topic ".in.size")
          sleep-time (-> config :console-progress :display-every)]
      (stoppable-thread
       "Console progress stats."
       (fn [{:keys [last-time last-count]
            :or   {last-time  (System/nanoTime)
                   last-count 0} :as data}]

         (let [time'  (System/nanoTime)
               count' (get (get-metric metric) :total 0)
               processed (- count' last-count)
               rate (double (/ processed (/ (- time' last-time) 1000000000)))]

           (when data
             (log/info (format "Processed %5d events at %5.0f/s" processed rate)))

           {:last-time time' :last-count count'}))
       :with-state true
       :sleep-time sleep-time))
    (fn [])))


(defn- init-tracking!
  "Initialises the metrics tracking system"
  [{enabled :enabled :as cfg}]
  (when enabled
    (log/info "Sending metrics to:" cfg)
    (set-base-metrics-name! "samsara" "core")
    (start-reporting! cfg)))



(defn init!
  "Initializes the system and returns the actual configuration used."
  [config-file]
  (let [config (read-config config-file)]

    (init-log!      (-> config :log))
    (init-tracking! (-> config :tracking))
    (samza/init-pipeline! config)

    config))



(defn- start-nrepl!
  "Start nREPL server."
  [{:keys [enabled port] :as cfg}]
  (if enabled
    (let [repl (nrepl/start-server :port port :handler cider/cider-nrepl-handler)]
      (log/info "Started nREPL on port:" port "..."))
    (log/info "nREPL disabled...!")))


(defn start-processing! [config-file]
  (let [{{:keys [input-topic input-partitions output-topic]}
         :topics :as config} (init! config-file)
         input-partitions (if (= input-partitions identity) :all input-partitions)]
    ;; starting nrepl
    (start-nrepl! (:nrepl config))
    ;; starting server
    (samza/start! config)
    (log/info "Samsara CORE processing started: " input-topic
              "/" input-partitions "->" output-topic)
    (show-console-progress! config)))


(defn -main
 [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options)            (do (help! nil) (exit! 1))
     (nil? (:config options))   (do (help! ["ERROR: Missing configuration."]) (exit! 1))
     errors                     (do (help! errors) (exit! 1))

     :default
     (do
       (println (headline))
       (start-processing! (:config options))))))



(comment

  ;; for REPL testing run this
  (def config (read-config "./config/config.edn"))
  (init-log!      (-> config :log))
  (init-tracking! (-> config :tracking))
  (samza/init-pipeline! config)

  ;; start samza consumer threads
  (samza/start! config)

  ;; or optionally process an event
  (def e1  {:timestamp (System/currentTimeMillis)
            :eventName "pipeline.tested"
            :sourceId "me"})

  ;; process the event
  ;; input:  [state  [events]]
  ;; output: [state' [events']]
  (samza/*pipeline* (moebius.kv/make-in-memory-kvstore) [e1])

  ;; process the event in its raw format
  ;; input:  [state  json-event-as-string]
  ;; output: [state' [[destination-topic partition-key json-event'-as-string]]
  (def e1s (samsara.utils/to-json e1))
  (samza/*raw-pipeline* (moebius.kv/make-in-memory-kvstore) e1s)
  )
