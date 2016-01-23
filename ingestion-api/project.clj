(defproject samsara/ingestion-api (-> "../samsara.version" slurp .trim)
  :description "Ingestion APIs for Samsara's analytics"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/ingestion-api"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [samsara/samsara-utils #=(clojure.string/trim #=(slurp "../samsara.version"))]
                 [aleph "0.4.0"]
                 [gloss "0.2.5"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.4.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [prismatic/schema "0.4.0"]
                 [clj-kafka "0.2.8-0.8.1.1"]
                 [com.taoensso/timbre "3.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [samsara/trackit "0.2.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.stuartsierra/component "0.3.0"]
                 [reloaded.repl "0.2.1"]]
  :main ingestion-api.core
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]
                                  [clojurewerkz/machine_head "1.0.0-beta9"]
                                  [clj-mqtt "0.4.1-alpha"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}
  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "ingestion-api" :bootclasspath false})
