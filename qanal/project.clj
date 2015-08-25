(defproject qanal "0.3.0-SNAPSHOT"
  :description "An Application that bulk indexes docs from Kafka to Elasticsearch"
  :url "https://github.com/samsara/qanal"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [com.taoensso/timbre "3.4.0"]
                 [clj-kafka "0.3.2"]
                 [clojurewerkz/elastisch "2.1.0"]
                 [cheshire "5.5.0"]
                 [prismatic/schema "0.4.4"]
                 [samsara/trackit "0.2.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.11.0"]]
  :main qanal.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.7.0"]
                                  [midje-junit-formatter "0.1.0-SNAPSHOT"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "qanal" :bootclasspath false}
  )
