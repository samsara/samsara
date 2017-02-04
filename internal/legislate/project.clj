(defn ver [] (-> "../samsara.version" slurp .trim))

(defproject samsara/legislate "0.1.0-SNAPSHOT"
  :description "FIXME: write description"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/ingestion-api"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]])
