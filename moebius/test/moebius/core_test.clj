(ns moebius.core-test
  (:require [moebius.core :refer :all])
  (:use midje.sweet))


(facts "about stateless `enricher`: results are normalised for the `cycler`"

       ;; enricher accepts a function which optionally perform
       ;; a transformation to the given event
       ((enricher (stateless identity))         1 {:a 1})    =>    [1  [{:a 1}]]

       ;; if the function changes the event, the new event must
       ;; be returned
       ((enricher (stateless #(assoc % :b 2)))  1 {:a 1})    =>    [1 [{:a 1 :b 2}]]

       ;; if the enrichment function return nil
       ;; then the event is left unchanged
       ((enricher (stateless (fn [x] nil)))      1 {:a 1})    =>    [1 [{:a 1}]]

       )



(facts "about stateless `correlator`: results are normalised for the `cycler`"

       ;; correlator accepts a function which optionally perform
       ;; a transformation to the given event and it can return
       ;; 0, 1 or more new events
       ((correlator (stateless (fn [x] [x])))   1 {:a 1})    =>     [1 [{:a 1}{:a 1}]]


       ;; if the correlation function return [nil], an array with
       ;; a nil element should be returned which will cause the
       ;; the moebius to filter the event out.
       ((correlator (stateless (fn [x] [nil])))  1  {:a 1})    =>    [1 [{:a 1} nil]]


       ;; if the correlator function return more than one element
       ;; then the list the list is preserved and added to the
       ;; element to process
       ((correlator (stateless (fn [x] [{:a 2} {:a 3}])))  1   {:a 1})  =>  [1 [{:a 1} {:a 2} {:a 3}]]


       ;; if the correlation function return nil, an array with
       ;; a nil element should be returned which will cause the
       ;; the moebius to filter the event out.
       ((correlator (stateless (fn [x] nil)))  1   {:a 1})    =>    [1 [{:a 1}]]


       ;; if a correlator function retrurn an event (a map)
       ;; rather than returning a list/vector and the result
       ;; is wrapped into an vector
       ((correlator (stateless (fn [x] x)))  1 {:a 1})    =>     [1 [{:a 1} {:a 1}]]
       )



(facts "about stateless `filterer`: results are normalised for the `cycler`"

       ;; filterer accepts a filtering predicate just like core/filter
       ;; if the predicate applied to the event is truthy then
       ;; the event is kept, otherwise if filtered out.
       ((filterer (stateless-pred :a)) 1 {:a 1})    =>     [1 [{:a 1}]]
       ((filterer (stateless-pred :b)) 1 {:a 1})    =>     [1 [nil]]

       )



(facts "about `cycler`: it applies the functions to all given events and expands the result"

       (cycler 1 (enricher (stateless identity)) [{:a 1}])
       =>  [1  [{:a 1}]]

       (cycler 1 (enricher (stateless #(assoc % :b 2))) [{:a 1} {:a 2}])
       =>  [1 [{:a 1 :b 2} {:a 2 :b 2} ]]

       ;; when :a is 2 then emits a.2, a.3, a.4
       (let [f (fn [{a :a :as e}]
                 (when (= a 2)
                   [(update-in e [:a] inc)
                    (update-in e [:a] (comp inc inc))]))]

         (cycler 1 (correlator (stateless f))
          [{:a 1} {:a 2} {:a 5}])
         => [1 [{:a 1} {:a 2} {:a 3} {:a 4} {:a 5}]])


       (cycler 1 (filterer (stateless-pred (comp even? :a)))
               [{:a 1} {:a 2} {:a 3} {:a 4} {:a 5}])
       => [1 [ {:a 2} {:a 4} ]]

       )



#_(facts "about `pipeline`: it composes your streaming function and maintain the specified order"

       ((pipeline (enricher identity)) {:a 1}) => [{:a 1}]

       (let [ba2  (enricher (fn [{a :a :as e}] (assoc e :b (* a 2))))
             wr1  (enricher (fn [e] (assoc e :w 1)))
             wr2  (enricher (fn [e] (assoc e :w 2)))
             nob4 (filterer (fn [{b :b :as e}] (not= 4 b)))
             cor  (correlator (fn [{a :a :as e}]
                                (when (= a 2)
                                  [(update-in e [:a] inc)
                                   (update-in e [:a] (comp inc inc))])))
             cor2 (correlator (fn [{a :a :as e}]
                                (when (= a 3)
                                  [{:a 5}
                                   {:a 6}])))
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



#_(facts "about `pipeline` you should be able to compose pipeline as well"


       ;; testing composition of pipelines with enrichers
       (let [e1 (enricher #(assoc % :e1 true))
             e2 (enricher #(assoc % :e2 true))]
         (cycler (pipeline
                  (pipeline e1)
                  (pipeline e2))
                 [{:a 1}]))
       => [{:a 1 :e1 true :e2 true}]


       ;; testing composition of pipelines with correlation
       (let [e1 (enricher #(assoc % :e1 true))
             e2 (enricher #(assoc % :e2 true))
             c1 (correlator #(when (:a %) {:c1 true}))
             c2 (correlator #(when (:a %) {:c2 true}))]
         (cycler (pipeline
                  (pipeline e1 c1)
                  (pipeline e2 c2))
                 [{:a 1}]))
       => [{:e2 true, :e1 true, :a 1}
           {:e2 true, :e1 true, :c2 true}
           {:e2 true, :e1 true, :c1 true}]


       ;; testing composition of pipelines with filters
       (let [e1 (enricher #(assoc % :e1 true))
             e2 (enricher #(assoc % :e2 true))
             c1 (correlator #(when (:a %) {:c1 true}))
             c2 (correlator #(when (:a %) {:c2 true}))
             f1 (filterer #(not= 1 (:a %)))]
         (cycler (pipeline
                  (pipeline e1 c1)
                  (pipeline e2 c2 f1))
                 [{:a 1}]))
       => [{:e2 true, :e1 true, :c2 true}
           {:e2 true, :e1 true, :c1 true}]


       ;; testing composition of deep pipelines
       (let [e1 (enricher #(assoc % :e1 true))
             e2 (enricher #(assoc % :e2 true))
             c1 (correlator #(when (:a %) {:c1 true}))
             c2 (correlator #(when (:a %) {:c2 true}))
             f1 (filterer #(not= 1 (:a %)))]
         (cycler (pipeline
                  e1
                  (pipeline
                   e2
                   (pipeline
                    c1
                    (pipeline
                     c2
                     (pipeline f1)))))
                 [{:a 1}]))
       => [{:e2 true, :e1 true, :c2 true}
           {:e2 true, :e1 true, :c1 true}]


       ;; testing composition of pipelines of pipelines
       (let [e1 (enricher #(assoc % :e1 true))
             e2 (enricher #(assoc % :e2 true))
             c1 (correlator #(when (:a %) {:c1 true}))
             c2 (correlator #(when (:a %) {:c2 true}))
             f1 (filterer #(not= 1 (:a %)))]
         (cycler (pipeline
                  (pipeline
                   (pipeline e1)
                   (pipeline e2))
                  (pipeline
                   (pipeline c1)
                   (pipeline c2))
                  (pipeline f1))
                 [{:a 1}]))
       => [{:e2 true, :e1 true, :c2 true}
           {:e2 true, :e1 true, :c1 true}]

       )



(facts "about `inject-if`: it injects the value to the event if the condition is truthy"

       (inject-if {:a 1} true :b 1) => {:a 1 :b 1}
       (inject-if {:a 1} false :b 1) => {:a 1}

       )



(facts "about `inject-as`: it injects a value with the given property name if the value isn't nil"

       (inject-as {:a 1} :b nil) => {:a 1}
       (inject-as {:a 1} :b 1  ) => {:a 1 :b 1}

       )



(facts "about `when-event-name-is`: if the eventName matches the given name, or names,
                then do something, otherwise lave the event unchanged"

       (let [event {:eventName "event1"}]


         ;; if the event name doesn't match don't do anything
         (when-event-name-is event "another-event"
                        (assoc event :b 2))
         => nil


         ;; if the event name matches then apply the body
         (when-event-name-is event "event1"
                        (assoc event :b 2))
         => {:eventName "event1" :b 2}


         ;; if the event name matches any of the given name then apply the body
         (when-event-name-is event ["event2" "event1" "event"]
                        (assoc event :b 2))
         => {:eventName "event1" :b 2}

         )
       )



(tabular
 (facts "about `when-event-match`: if the match any of the conditions, them apply the corresponding expression,
                otherwise leve the event unchanged"

       (let [event ?event]
         (when-event-match event
                           [{:eventName "game.started" :level 0}]               (assoc event :new-player true)
                           [{:eventName _ :level (_ :guard #(= 0 (mod % 11)))}] (assoc event :level-type :extra-challenge)
                           [{:eventName "game.new.level" :level _}]             (assoc event :level-type :normal)
                           [{:eventName _ :level (_ :guard even?)}]             (assoc event :start :even-level)))
       =>     ?result )

 ?event                                               ?result
 {:eventName "game.started" :level 0 :device "x1"}    {:eventName "game.started" :level 0 :device "x1" :new-player true}
 {:eventName "game.started" :level 8}                 {:eventName "game.started" :level 8 :start :even-level}
 {:eventName "game.new.level" :level 33}              {:eventName "game.new.level" :level 33 :level-type :extra-challenge}
 {:eventName "game.new.level" :level 32}              {:eventName "game.new.level" :level 32 :level-type :normal}
 {:eventName "game.resume.level" :level 32}           {:eventName "game.resume.level" :level 32 :start :even-level}
 {:eventName "something.not.matching" :level 5}       nil
 )



#_(facts "about `moebius`: it composes your streaming functions and produce a function which applied
                to the events will process all events with the given pipeline"

       ((moebius (enricher identity)) [{:a 1}]) => [{:a 1}]

       (let [ba2  (enricher (fn [{a :a :as e}] (assoc e :b (* a 2))))
             wr1  (enricher (fn [e] (assoc e :w 1)))
             wr2  (enricher (fn [e] (assoc e :w 2)))
             nob4 (filterer (fn [{b :b :as e}] (not= 4 b)))
             cor  (correlator (fn [{a :a :as e}]
                                (when (= a 2)
                                  [(update-in e [:a] inc)
                                   (update-in e [:a] (comp inc inc))])))
             cor2 (correlator (fn [{a :a :as e}]
                                (when (= a 3)
                                  [{:a 5}
                                   {:a 6}])))
             events [{:a 1} {:a 2} {:a 7}]]




         ;;
         ;; Streaming functiona sre composable via `pipeline`
         ((moebius wr1 ba2 cor cor2 wr2 nob4) [{:a 1} {:a 2} {:a 7}])
         => [{:a 1 :b 2 :w 2} {:a 3 :b 6 :w 2} {:a 5 :b 10 :w 2} {:a 6 :b 12 :w 2}  {:a 4 :b 8 :w 2} {:a 7 :b 14 :w 2}]


         ;; order matters (from left-right)
         ((moebius wr1 wr2) [{:a 1} {:b 4} {:a 2}])
         => [{:a 1 :w 2} {:b 4 :w 2} {:a 2 :w 2}]

         ((moebius wr2 wr1) [{:a 1} {:b 4} {:a 2}])
         => [{:a 1 :w 1} {:b 4 :w 1} {:a 2 :w 1}]

         ))



(tabular
   (facts "about `match-glob`: globs should simplify name matching"

         (match-glob ?glob  ?name)    =>     ?result )

   ?glob                    ?name                         ?arrow    ?result
   "game.started"           "game.started"                   =>      truthy
   "game.started"           "game_started"                   =>      falsey
   "game.*"                 "game.started"                   =>      truthy
   "game.*"                 "game.level.started"             =>      falsey
   "game.**"                "game.level.started"             =>      truthy
   "game.**.started"        "game.level.started"             =>      truthy
   "game.**.started"        "game.level.2.started"           =>      truthy
   "game.**.started"        "game.level.3.stopped"           =>      falsey
   "game.**.started"        "mygame.level.4.started"         =>      falsey
   "game.**.started"        "game.level.5.started2"          =>      falsey

 )
