(defn ver [] (-> "../../samsara.version" slurp .trim))

(defproject samsara/samsara-client (ver)
  :description "Clojure client for Samsara"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/clients/clojure"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [samsara/samsara-utils #=(ver)]
                 [prismatic/schema "1.0.4"]
                 [clj-http "2.0.0"]
                 [amalloy/ring-buffer "1.2"]
                 [com.stuartsierra/component "0.3.1"]]

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.2"]]
                   :plugins [[lein-midje "3.2"]]}})
