(ns samsara.scribe.edn-test
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.edn :as edn]
             [samsara.scribe.protocol :as scribe]))


(facts "EDN serializer"

  (fact "roundtrip edn"

    (let [s (edn/make-edn-scribe)
          data {:test 1
                :foo "bar"
                :valid? true
                :date "2017-02-04T19:41:53.206Z"
                :value 2.3}]

      (->> (scribe/write s data)
        (scribe/read s))
      => data))

  (fact "roundtrip nil"

    (let [s (edn/make-edn-scribe)
          data nil]

      (->> (scribe/write s data)
        (scribe/read s))
      => data))

  (fact "roundtrip record"

    (defrecord Event [number owner])

    (let [event-reader {'samsara.scribe.edn_test.Event map->Event}
          s (edn/make-edn-scribe {:readers event-reader})
          data (->Event 123, "John Doe")]

      (->>
        (scribe/write s data)
        (scribe/read s))
      => data)))
