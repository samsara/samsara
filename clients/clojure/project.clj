(defproject samsara/samsara-client (-> "../../samsara.version" slurp .trim)
  :description "Clojure client for Samsara"
  :url "http://samsara-analytics.io/"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [samsara/samsara-utils #=(clojure.string/trim #=(slurp "../../samsara.version"))]
                 [prismatic/schema "1.0.4"]
                 [clj-http "2.0.0"]
                 [amalloy/ring-buffer "1.2"]
                 [com.stuartsierra/component "0.3.1"]]

  :deploy-repositories[["clojars" {:url "https://clojars.org/repo/"
                                   :sign-releases true}]]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.2"]]
                   :plugins [[lein-midje "3.2"]]}})
