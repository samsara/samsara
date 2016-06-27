(defproject samsara/samsara-core (-> "../samsara.version" slurp .trim)
  :description "Event stream processing pipeline"

  :url "https://samsara.github.com/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/core"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :java-source-paths ["java"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [org.apache.samza/samza-api        "0.9.1"]
                 [org.apache.samza/samza-kafka_2.10 "0.9.1"]
                 [samsara/moebius
                  #=(clojure.string/trim #=(slurp "../samsara.version"))]
                 [samsara/samsara-utils
                  #=(clojure.string/trim #=(slurp "../samsara.version"))]
                 [samsara/trackit "0.3.0"]
                 [com.taoensso/timbre "4.0.2"]
                 [org.clojure/tools.cli "0.3.1"]
                 [digest "1.4.4"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [clj-kafka "0.3.2"]
                 [com.stuartsierra/component "0.3.0"]]

  :main samsara-core.main

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]
                             [lein-shell "0.5.0"]]}}

  :aliases {"docker"
            ["shell" "docker" "build" "-t" "samsara/samsara-core:${:version}" "."]

            "docker-latest"
            ["shell" "docker" "build" "-t" "samsara/samsara-core" "."]}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "samsara-core" :bootclasspath false})
