(ns samsara.scribe.nippy-test
  (:require [midje.sweet :refer :all]
            [samsara.scribe.serializer.nippy :as nippy]
            [samsara.scribe.protocol :as scribe]))


(facts "Nippy serializer"

  (fact "roundtrip nippy"

    (let [s (nippy/make-nippy-scribe)
          data {:test 1
                :foo "bar"
                :date "2017-02-04T19:41:53.206Z"
                :valid? true
                :value 2.3}]

    (->> (scribe/write s data)
      (scribe/read s))
    => data))

  (fact "roundtrip nil"

    (let [s (nippy/make-nippy-scribe)
          data nil]

      (->> (scribe/write s data)
        (scribe/read s))
      => data)))
