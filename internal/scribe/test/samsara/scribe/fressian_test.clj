(ns samsara.scribe.fressian-test
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.fressian :as fressian]
             [samsara.scribe.protocol :as scribe]))


(facts "Fressian serializer"

  (fact "roundtrip json"

    (let [s   (fressian/make-fressian-scribe)
          data {:test 1
                :foo "bar"
                :date "2017-02-04T19:41:53.206Z"
                :valid? true
                :value 2.3}]

      (->> (scribe/write s data)
        (scribe/read  s))
      => data))


  (fact "roundtrip nil"

    (let [s   (fressian/make-fressian-scribe)
          data nil]

      (->> (scribe/write s data)
        (scribe/read  s))
      => data)))
