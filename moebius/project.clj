(defproject samsara/moebius (-> "../samsara.version" slurp .trim)
  :description "A system to process and enrich and correlate events in realtime"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/moebius"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [prismatic/schema "0.4.3"]                ;; schema validation
                 [com.taoensso/timbre "4.0.2"]             ;; logging
                 ;;[samsara/trackit "0.2.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]   ;; pattern-matching
                 [potemkin "0.3.13"]                       ;; import-var
                 [com.brunobonacci/where "0.1.0"]          ;; where
                 [rhizome "0.2.5"]                         ;; graphviz
                 ]


  :profiles {:dev {:dependencies [[midje "1.7.0"]
                                  [org.clojure/test.check "0.8.2"]]
                   :plugins [[lein-midje "3.1.3"]]}}

  :aliases {"test-check" ["midje" ":filters" "test-check"]
            "test"       ["midje"]
            "quick-test" ["midje" ":filters" "-slow"]})
