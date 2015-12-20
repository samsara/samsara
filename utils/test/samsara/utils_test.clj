(ns samsara.utils-test
  (:require [midje.sweet :refer :all]
            [samsara.utils :refer :all]))



(fact "gzip string can roundtrip correctly"

      (let [roundtrip       (comp ungzip-string gzip-string)
            check-roundtrip (fn [x] (= x (roundtrip x)))]

        (check-roundtrip nil) => truthy
        (check-roundtrip "") => truthy
        (check-roundtrip "small string") => truthy
        (check-roundtrip (apply str (range 1000))) => truthy
        ))
