(defproject samsara/samsara-builtin-modules "0.1.0-SNAPSHOT"
  :description "Samsara built-in modules"
  :url "https://samsara.github.com/"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [samsara/moebius "0.2.0"]]

  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                    :sign-releases false}]])
