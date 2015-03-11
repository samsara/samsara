(defproject ingestion-api "0.1.0-SNAPSHOT"
  :description "Ingestion APIs for Samsara's analytics"
  :url "https://samsara.github.com/"
  :license {:name "Apache 2 License"
            :url "http://www.apache.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.19"]
                 [ring/ring-devel "1.3.2"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.3.2"]
                 [javax.servlet/servlet-api "2.5"]
                 [cheshire "5.4.0"]
                 [clj-kafka "0.2.8-0.8.1.1"]
                 [com.taoensso/timbre "3.4.0"]
                 [org.clojure/tools.cli "0.3.1"]]

  :main ingestion-api.core
  :profiles
  {:uberjar {:aot :all}})
