(defn ver [] (-> "../samsara.version" slurp .trim))

(defproject samsara/moebius (ver)
  :description "A system to process and enrich and correlate events in realtime"

  :url "http://samsara-analytics.io/"

  :scm {:name "github" :url "https://github.com/samsara/samsara/tree/master/moebius"}

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema "0.4.3"]                ;; schema validation
                 [com.taoensso/timbre "4.5.1"]             ;; logging
                 ;;[samsara/trackit-core "0.5.2"]
                 [org.clojure/core.match "0.3.0-alpha4"]   ;; pattern-matching
                 [potemkin "0.4.3"]                        ;; import-var
                 [com.brunobonacci/where "0.5.0"]          ;; where
                 [rhizome "0.2.5"]                         ;; graphviz
                 ]


  :profiles {:dev {:dependencies [[midje "1.7.0"]
                                  [org.clojure/test.check "0.8.2"]]
                   :plugins [[lein-midje "3.1.3"]]}}

  :jvm-opts ["-server" "-Dfile.encoding=utf-8"]

  :aliases {"test-check" ["midje" ":filters" "test-check"]
            "test"       ["midje"]
            "quick-test" ["midje" ":filters" "-slow"]})
