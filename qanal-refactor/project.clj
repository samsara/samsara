(defn ver [] (-> "../samsara.version" slurp .trim))

(defproject samsara/qanal (ver)
  :description "An Application that bulk indexes docs from Kafka to Elasticsearch"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/qanal"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [com.taoensso/timbre "4.7.0"]
                 [prismatic/schema "0.4.4"]
                 [com.brunobonacci/safely "0.2.1"]
                 [curator "0.0.6"]
                 [com.dayooliyide/kafkian "0.1.0"]]

  :main samsara.qanal.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.7.0"]
                                  [midje-junit-formatter "0.1.0-SNAPSHOT"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]
                             [lein-shell "0.5.0"]]}}

  :aliases {"docker"
            ["shell" "docker" "build" "-t" "samsara/qanal:${:version}" "."]

            "docker-latest"
            ["shell" "docker" "build" "-t" "samsara/qanal" "."]}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "qanal" :bootclasspath false}
  )
