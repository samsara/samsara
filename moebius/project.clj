(defproject moebius "0.1.0-SNAPSHOT"
  :description "A system to process and enrich and correlate events in realtime"

  :url "https://samsara.github.com/"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [prismatic/schema "0.4.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [samsara/trackit "0.2.0"]
                 [samsara/samsara-utils "0.1.0-SNAPSHOT"]
                 [org.clojure/core.match "0.3.0-alpha4"]]

  :main moebius.core

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-bin "0.3.5"]]}}
)
