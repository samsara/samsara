(defproject samsara-logger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.slf4j/slf4j-api "1.7.12"]]
  :aot [org.slf4j.impl.SamsaraLogger
        org.slf4j.impl.SamsaraLoggerFactory]

  :source-paths ["src"]
  :java-source-paths ["srcj"]
  :prep-tasks ["compile" "javac"])





