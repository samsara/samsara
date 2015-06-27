(defproject samsara-core "0.2.0-SNAPSHOT"
  :description "Event stream processing pipeline"

  :url "https://samsara.github.com/"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :java-source-paths ["java"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.samza/samza-api        "0.9.0"]
                 [org.apache.samza/samza-kafka_2.10 "0.9.0"]
                 [samsara/moebius "0.2.0"]
                 [samsara/samsara-utils "0.3.0"]
                 [samsara/trackit "0.2.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [digest "1.4.4"]
                 ]

  :main samsara-core.main

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "samsara-core" :bootclasspath false})
