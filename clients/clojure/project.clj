(defproject samsara/samsara-client "0.1.4"
  :description "Clojure SDK for Samsara"
  :url "http://samsara.github.io"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [samsara/samsara-utils "0.1.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [http-kit "2.1.16"]
                 #_[prismatic/schema "0.4.0"]
                 [amalloy/ring-buffer "1.0"]
                 [jarohen/chime "0.1.6"]

                 #_[org.clojure/clojure "1.7.0"]
                 #_[samsara/samsara-utils "0.4.0"]
                 [prismatic/schema "1.0.4"]
                 [clj-http "2.0.0"]

                 ]
  :target-path "target/%s"
  :plugins [[lein-midje "3.2"]]
  :deploy-repositories[["clojars" {:url "https://clojars.org/repo/"
                                   :sign-releases true}]]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.2"]]}})
