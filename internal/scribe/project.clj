(defn ver [] (-> "../../samsara.version" slurp .trim))

(defproject samsara/scribe (ver)
  :description "A solution to write and read Clojure data in various formats"

  :url "http://samsara-analytics.io/"

  :scm {:name "github"
        :url "https://github.com/samsara/samsara/tree/master/internal/scribe"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.7.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [org.clojure/data.fressian "0.2.1"]
                 [com.cognitect/transit-clj "0.8.297"]]

  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]]
                   :plugins [[lein-midje "3.2.1"]]}}

  )
