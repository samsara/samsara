(defproject qanal "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]            ;Command line parsing
                 [org.clojure/tools.logging "0.3.1"]        ;logging
                 [org.slf4j/slf4j-api "1.6.4"]              ;logging
                 [clj-kafka "0.2.8-0.8.1.1"]                ;kafka
                 [clojurewerkz/elastisch "2.1.0"]           ;elasticsearch
                 [cheshire "5.4.0"]                         ;JSON
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"] ;Core Async
                 [com.novemberain/validateur "2.4.2"]       ;validation
                 ]
  :main kasiphones.core
  :aot [kasiphones.core]


  :profiles {:dev {:dependencies [[org.slf4j/slf4j-simple "1.6.4"]]
                   :jvm-opts ["-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"]}
             :uberjar {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]]}}


  )
