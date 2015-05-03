(ns moebius.core-test
  (:require [moebius.core :refer :all])
  (:use midje.sweet))


(facts "about `enricher`: results are normalised for the `cycler`"

       ;; enricher accepts a function which optionally perform
       ;; a transformation to the given event
       ((enricher identity)        {:a 1})    =>     [{:a 1}]

       ;; if the function changes the event, the new event must
       ;; be returned
       ((enricher #(assoc % :b 2)) {:a 1})    =>     [{:a 1 :b 2}]

       ;; if the enrichment function return nil, then an empty
       ;; array should be returned which will cause the
       ;; the moebius to filter the event out.
       ((enricher (fn [x] nil))     {:a 1})    =>     []

       )


(facts "about `correlator`: results are normalised for the `cycler`"

       ;; correlator accepts a function which optionally perform
       ;; a transformation to the given event and it can return
       ;; 0, 1 or more events
       ((correlator (fn [x] [x]))  {:a 1})    =>     [{:a 1}]

       ;; if the function changes the event, the new event must
       ;; be returned
       ((correlator #(vector (assoc % :b 2))) {:a 1})    =>     [{:a 1 :b 2}]

       ;; if the correlation function return nil, then an empty
       ;; array should be returned which will cause the
       ;; the moebius to filter the event out.
       ((correlator (fn [x] nil))     {:a 1})    =>     []

       )


(facts "about `filterer`: results are normalised for the `cycler`"

       ;; filterer accepts a filtering predicate just like core/filter
       ;; if the predicate applied to the event is truthy then
       ;; the event is kept, otherwise if filtered out.
       ((filterer :a)  {:a 1})    =>     [{:a 1}]
       ((filterer :b)  {:a 1})    =>     []

       )


(facts "about `cycler`: it applies the functions to all given events and expands the result"

       (cycler (enricher identity) [{:a 1}])
       =>  [{:a 1}]

       (cycler (enricher #(assoc % :b 2)) [{:a 1} {:a 2}])
       =>  [{:a 1 :b 2} {:a 2 :b 2} ]

       ;; when :a is 2 then emit a.2, a.3, a.4
       (let [f (fn [{a :a :as e}]
                 (if (= a 2)
                   [e
                    (update-in e [:a] inc)
                    (update-in e [:a] (comp inc inc))]
                   [e]))]

         (cycler (correlator f)
          [{:a 1} {:a 2} {:a 5}])
         => [{:a 1} {:a 2} {:a 3} {:a 4} {:a 5}])


       (cycler (filterer (comp even? :a))
               [{:a 1} {:a 2} {:a 3} {:a 4} {:a 5}])
       => [ {:a 2} {:a 4} ]

       )
