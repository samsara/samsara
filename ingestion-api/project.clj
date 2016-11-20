(defn ver [] (-> "../samsara.version" slurp .trim))

(defproject samsara/ingestion-api (ver)
  :description "Ingestion APIs for Samsara's analytics"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/ingestion-api"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [samsara/samsara-utils #=(ver)]
                 [aleph "0.4.1"]
                 [gloss "0.2.5"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.4.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [prismatic/schema "0.4.0"]
                 [clj-kafka "0.2.8-0.8.1.1"]
                 [com.taoensso/timbre "4.5.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [samsara/trackit "0.3.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.stuartsierra/component "0.3.0"]
                 [ns-tracker "0.3.0"]
                 [com.brunobonacci/synapse "0.3.2"]
                 [samsara-mqtt "0.1.0"]]

  :main ingestion-api.main

  :profiles {:uberjar {:aot :all :resource-paths ["../samsara.version"]}
             :dev {:dependencies [[midje "1.6.3"]
                                  [clj-http "2.1.0"]
                                  [clojurewerkz/machine_head "1.0.0-beta9"]
                                  [reloaded.repl "0.2.1"]
                                  [clj-mqtt "0.4.1-alpha"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]
                             [lein-shell "0.5.0"]]}}

  :aliases {"docker"
            ["shell" "docker" "build" "-t" "samsara/ingestion-api:${:version}" "."]

            "docker-latest"
            ["shell" "docker" "build" "-t" "samsara/ingestion-api" "."]

            "docker-snapshot"
            ["shell" "docker" "build" "-t" "samsara/ingestion-api:snapshot" "."]}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "ingestion-api" :bootclasspath false}
  )
