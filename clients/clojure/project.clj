(defproject samsara/samsara-client "0.1.1"
  :description "Clojure SDK for Samsara"
  :url "http://samsara.github.io"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [samsara/samsara-utils "0.1.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [http-kit "2.1.16"]
                 [prismatic/schema "0.4.0"]
                 [amalloy/ring-buffer "1.0"]
                 [jarohen/chime "0.1.6"]]
  :target-path "target/%s"
  :plugins [[lein-pprint "1.1.1"]
            [lein-midje "3.1.3"]]
  :deploy-repositories[["clojars" {:url "https://clojars.org/repo/"
                                   :sign-releases false}]]
  :profiles {:uberjar {:aot :all}
             :dev {
                   :dependencies [[http-kit.fake "0.2.1"]
                                  [midje "1.6.3"]]}})
