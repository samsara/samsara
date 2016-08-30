(defproject samsara/samsara-builtin-modules (-> "../samsara.version" slurp .trim)
  :description "Samsara built-in modules"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/modules"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [samsara/moebius #=(clojure.string/trim #=(slurp "../samsara.version"))]
                 [digest "1.4.4"]
                 [com.brunobonacci/ip-geoloc "0.2.0-SNAPSHOT"]]

  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                    :sign-releases false}]])
