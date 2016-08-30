(defproject samsara/samsara-logger (-> "../../samsara.version" slurp .trim)
  :description "A logging interface for sending logs as events to samsara"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/clients/logger"}

  :license {:name "Apache License V2"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [samsara/samsara-client #=(clojure.string/trim #=(slurp "../../samsara.version"))]]
  :aot [samsara.logger.EventLogger samsara.logger.EventLoggerBuilder]

  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["test/main/clojure"]

  :prep-tasks ["compile" "javac"]
  )
