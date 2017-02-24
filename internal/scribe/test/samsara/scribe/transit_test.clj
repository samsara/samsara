(ns samsara.scribe.transit-test
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.transit :as transit]
             [samsara.scribe.protocol :as scribe]))


(facts "Transit serializer"

  (fact "roundtrip json"

    (let [s (transit/make-transit-scribe)
          data {:test 1
                :foo "bar"
                :date "2017-02-04T19:41:53.206Z"
                :valid? true
                :value 2.3}]

      (->> (scribe/write s data)
        (scribe/read  s))
      => data))


  (fact "roundtrip nil"

    (let [s (transit/make-transit-scribe)
          data nil]

      (->> (scribe/write s data)
        (scribe/read  s))
      => data)))
