(defproject ingestion-api "0.3.0-SNAPSHOT"
  :description "Ingestion APIs for Samsara's analytics"
  :url "https://samsara.github.com/"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.19"]
                 [ring/ring-devel "1.3.2"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.3.3"]
                 [javax.servlet/servlet-api "2.5"]
                 [prismatic/schema "0.4.0"]
                 [clj-kafka "0.2.8-0.8.1.1"]
                 [com.taoensso/timbre "3.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [samsara/trackit "0.2.0"]
                 [samsara/samsara-utils "0.1.0"]]

  :main ingestion-api.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]
  :bin {:name "ingestion-api" :bootclasspath false})
