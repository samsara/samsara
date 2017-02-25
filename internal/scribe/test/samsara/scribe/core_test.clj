(ns samsara.scribe.core-test
  (:refer-clojure :exclude [read])
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.edn :as edn]
             [samsara.scribe.serializer.fressian :as fressian]
             [samsara.scribe.serializer.nippy :as nippy]
             [samsara.scribe.serializer.json :as json]
             [samsara.scribe.serializer.transit :as transit]

             [samsara.scribe.core :refer :all]
             [samsara.scribe.protocol :as scribe-protocol]))


(facts "Scribe Core"

  (def serializer-types [:edn :transit :fressian :nippy :json])

  (def serializers (map scribe (map #({:type %}) serializer-types)))

  (tabular
    (fact "Uses serializer of a proper type based on given config"
      (type (scribe ?config)) => ?expected-type)
    ?config                    ?expected-type
    {:type :edn}               samsara.scribe.serializer.edn.EdnScribe
    {:type :transit}           samsara.scribe.serializer.transit.TransitScribe
    {:type :fressian}          samsara.scribe.serializer.fressian.FressianScribe
    {:type :nippy}             samsara.scribe.serializer.nippy.NippyScribe
    {:type :json}              samsara.scribe.serializer.json.JsonScribe)


  (tabular
    (fact "Its serializer can retain default config values"
      (-> (scribe ?input) :config) => ?config)
    ?input                ?config
    {:type :edn}          {}
    {:type :transit}      {:size 1024 :format :json}
    {:type :json}         {:date-format "yyyy-MM-dd'T'HH:mm:ss.SSSX"})


  (tabular
    (fact "Its serializer can get overridden config values"
      (-> (scribe ?input) :config) => ?config)
    ?input                                              ?config
    {:type :edn :reader :some-reader}                   {:reader :some-reader}
    {:type :transit :size 50 :format :bson}             {:size 50 :format :bson}
    {:type :json :date-format "yyyy-MM-dd" :foo :bar}   {:date-format "yyyy-MM-dd" :foo :bar}))
