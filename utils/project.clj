(defproject samsara/samsara-utils (-> "../samsara.version" slurp .trim)
  :description "Common utilities from Samsara's project"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/utils"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [com.brunobonacci/safely "0.2.1"]]

  :profiles {:dev {:dependencies [[midje "1.8.2"]]
                   :plugins [[lein-midje "3.2"]]}}

  )
