(ns samsara.scribe.edn-test
  (:require  [midje.sweet :refer :all]
             [samsara.scribe.serializer.edn :as edn]
             [samsara.scribe.protocol :as scribe]))


(facts "EDN serializer"

  (fact "roundtrip edn"

    (let [s (edn/make-edn-scribe)
          data {:test 1
                :foo "bar"
                :keyword :quux
                :symbol 'a
                :set #{1 2 3}
                :map {:foo "bar" :two 2}
                :vector ["I" "am" "vector"]
                :list '(1 4 6)
                :valid? true
                :date (java.util.Date.)
                :character \space
                :ratio 22/7
                :integer 5000
                :bigint 19931029N
                :float -2.3
                :bigdecimal 3.1415M}]

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
