(defproject qanal "0.1.0"
  :description "An Application that bulk indexes docs from Kafka to Elasticsearch"
  :url "https://github.com/samsara/qanal"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]            ;Command line parsing
                 [com.taoensso/timbre "3.3.1"]              ;logging
                 [clj-kafka "0.2.8-0.8.1.1"]                ;kafka
                 [clojurewerkz/elastisch "2.1.0"]           ;elasticsearch
                 [cheshire "5.4.0"]                         ;JSON
                 [prismatic/schema "0.4.0"]                 ;validation
                 [metrics-clojure "2.2.0"]                  ;Metrics Clojure wrapper
                 [riemann-clojure-client "0.3.2"]           ;Riemann client AND Metric Reporters
                 ]
  :main qanal.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "qanal" :bootclasspath false}
  )
