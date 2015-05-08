(defproject samsara/samsara-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/timbre "3.4.0"]
                 [http-kit "2.1.16"]
                 [com.novemberain/validateur "2.4.2"]
                 [amalloy/ring-buffer "1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [jarohen/chime "0.1.6"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {
                   :dependencies [[http-kit.fake "0.2.1"]
                                  [midje "1.6.3"]
                                   ]}})
