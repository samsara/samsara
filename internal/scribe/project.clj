(defn ver [] (-> "../../samsara.version" slurp .trim))

(defproject samsara/scribe (ver)
  :description "A solution to write and read Clojure data in various formats"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/internal/scribe"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.7.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  )
