(defn ver [] (-> "../../samsara.version" slurp .trim))

(defproject samsara/marchina (ver)
  :description "A library for state machines management"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/internal/scribe"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]
                                  [criterium "0.4.4"]]
                   :plugins [[lein-midje "3.2.1"]]}})
