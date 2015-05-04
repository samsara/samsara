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
       ((enricher (fn [x] nil))     {:a 1})    =>     [nil]

       )


(facts "about `correlator`: results are normalised for the `cycler`"

       ;; correlator accepts a function which optionally perform
       ;; a transformation to the given event and it can return
       ;; 0, 1 or more events
       ((correlator (fn [x] [x]))  {:a 1})    =>     [{:a 1}]

       ;; if the function changes the event, the new event must
       ;; be returned
       ((correlator #(vector (assoc % :b 2))) {:a 1})    =>     [{:a 1 :b 2}]

       ;; if the correlation function return nil, an array with
       ;; a nil element should be returned which will cause the
       ;; the moebius to filter the event out.
       ((correlator (fn [x] [nil]))     {:a 1})    =>     [nil]


       ;; if the correlator function return more than one element
       ;; then the list the list is preserved and added to the
       ;; element to process
       ((correlator (fn [x] [{:a 1} {:a 2} {:a 3}]))     {:a 1})    =>     [{:a 1} {:a 2} {:a 3}]

       )


(facts "about `filterer`: results are normalised for the `cycler`"

       ;; filterer accepts a filtering predicate just like core/filter
       ;; if the predicate applied to the event is truthy then
       ;; the event is kept, otherwise if filtered out.
       ((filterer :a)  {:a 1})    =>     [{:a 1}]
       ((filterer :b)  {:a 1})    =>     [nil]

       )


(facts "about `cycler`: it applies the functions to all given events and expands the result"

       (cycler (enricher identity) [{:a 1}])
       =>  [{:a 1}]

       (cycler (enricher #(assoc % :b 2)) [{:a 1} {:a 2}])
       =>  [{:a 1 :b 2} {:a 2 :b 2} ]

       ;; when :a is 2 then emits a.2, a.3, a.4
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


(facts "about `pipeline`: pipline compose your streaming function and maintain the specified order"

       ((pipeline (enricher identity)) {:a 1}) => [{:a 1}]

       (let [ba2  (enricher (fn [{a :a :as e}] (assoc e :b (* a 2))))
             wr1  (enricher (fn [e] (assoc e :w 1)))
             wr2  (enricher (fn [e] (assoc e :w 2)))
             nob4 (filterer (fn [{b :b :as e}] (not= 4 b)))
             cor  (correlator (fn [{a :a :as e}]
                                (if (= a 2)
                                  [e
                                   (update-in e [:a] inc)
                                   (update-in e [:a] (comp inc inc))]
                                  [e])))
             cor2 (correlator (fn [{a :a :as e}]
                                (if (= a 3)
                                  [e
                                   {:a 5}
                                   {:a 6}]
                                  [e])))
             events [{:a 1} {:a 2} {:a 7}]]




         ;;
         ;; Streaming functiona sre composable via `pipeline`
         (cycler (pipeline wr1 ba2 cor cor2 wr2 nob4) [{:a 1} {:a 2} {:a 7}])
         => [{:a 1 :b 2 :w 2} {:a 3 :b 6 :w 2} {:a 5 :b 10 :w 2} {:a 6 :b 12 :w 2}  {:a 4 :b 8 :w 2} {:a 7 :b 14 :w 2}]


         ;; order matters (from left-right)
         (cycler (pipeline wr1 wr2) [{:a 1} {:b 4} {:a 2}])
         => [{:a 1 :w 2} {:b 4 :w 2} {:a 2 :w 2}]

         (cycler (pipeline wr2 wr1) [{:a 1} {:b 4} {:a 2}])
         => [{:a 1 :w 1} {:b 4 :w 1} {:a 2 :w 1}]

         ))
