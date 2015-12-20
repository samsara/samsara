(defproject samsara/samsara-utils "0.5.0-SNAPSHOT"
  :description "Common utilities from Samsara's project"

  :url "https://samsara.github.com/"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [com.taoensso/timbre "3.4.0"]]

  :profiles {:dev {:dependencies [[midje "1.8.2"]]
                   :plugins [[lein-midje "3.2"]]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                   :sign-releases false}]])
