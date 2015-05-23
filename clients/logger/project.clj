(defproject samsara-logger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [samsara/samsara-client "0.1.0-beta2"]]
  :aot [samsara.logger.core
        samsara.slf4j.SamsaraLogger
        samsara.slf4j.SamsaraLoggerFactory
        samsara.log4j2.SamsaraLogger]

  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"]
  :test-paths ["test/main/clojure"]

  :prep-tasks ["compile" "javac"])





