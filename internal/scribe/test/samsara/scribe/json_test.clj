(ns samsara.scribe.json-test
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.json :as json]
             [samsara.scribe.protocol :as scribe]))


(facts "JSON serializer"

       (fact "roundtrip json"

             (let [s   (json/make-json-scribe)
                   data {:test 1
                         :foo "bar"
                         :date "2017-02-04T19:41:53.206Z"
                         :valid? true
                         :value 2.3}]

               (->> (scribe/write s data)
                    (scribe/read  s))
               => data))



       (fact "roundtrip nil"

             (let [s   (json/make-json-scribe)
                   data nil]

               (->> (scribe/write s data)
                    (scribe/read  s))
               => data)))
