(ns samsara-core.main
  (:require [samsara-core.samza :as samza])
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log])
  (:require [samsara.trackit :refer [start-reporting! set-base-metrics-name!]])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl :as cider])
  (:gen-class))


(def DEFAULT-CONFIG
  {:topics
   {:job-name "Samsara"
    :input-topic "ingestion"
    ;; :kvstore-topic "ingestion-kv"
    :output-topic "events"
    :output-topic-partition-fn (comp :sourceId :source)
    ;; a CSV list of hosts and ports (and optional path)
    :zookeepers "127.0.0.1:2181"
    ;; a CSV list of host and ports of kafka brokers
    :brokers "127.0.0.1:9092"
    :offset :smallest}

   ;; this section controls the indexing strategy
   :index
   {
    ;; strategy can be either :single or :daily
    ;; if daily the base-index will be used as prefix
    ;; and date will appened eg: events-2015-05-27
    :strategy :single
    :base-index "events"
    :event-type "events"}

   ;; metrics trackings
   :tracking
   {:enabled false :type :console}

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
      (merge-with merge DEFAULT-CONFIG)))


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


(defn -main
 [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options)            (do (help! nil) (exit! 1))
     (nil? (:config options))   (do (help! ["ERROR: Missing configuration."]) (exit! 1))
     errors                     (do (help! errors) (exit! 1))

     :default
     (let [_ (println (headline))
           {config-file :config} options
           {{:keys [input-topic output-topic]} :topics :as config} (init! config-file)]
       ;; starting nrepl
       (start-nrepl! (:nrepl config))
       ;; starting server
       (samza/start! config)
       (log/info "Samsara CORE processing started: " input-topic "->" output-topic)))))



(comment

  ;; for REPL testing run this
  (def config (read-config "./config/config.edn"))
  (init-log!      (-> config :log))
  (init-tracking! (-> config :tracking))
  (samza/init-pipeline! config)

  ;; start samza consumer threads
  (samza/start! config)

  )
