(defproject qanal "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]            ;Command line parsing
                 [com.taoensso/timbre "3.3.1"]              ;logging
                 [clj-kafka "0.2.8-0.8.1.1"]                ;kafka
                 [clojurewerkz/elastisch "2.1.0"]           ;elasticsearch
                 [cheshire "5.4.0"]                         ;JSON
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"] ;Core Async
                 [com.novemberain/validateur "2.4.2"]       ;validation
                 ]
  :main qanal.core
  :aot [qanal.core]



  )
