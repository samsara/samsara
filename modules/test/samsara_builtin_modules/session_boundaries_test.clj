(ns samsara-builtin-modules.session-boundaries-test
  (:require [clojure.test :refer :all]
            [samsara-builtin-modules.session-boundaries :refer :all])
  (:use midje.sweet)
  (:require [clojure.string :as s])
  (:require [moebius.core :refer :all]
            [moebius.kv :as kv]))

(def proc (moebius session-boundaries-correlation))



(fact "if the event is not a started/stopped nothing should be changed
       and no event should be generated"

      (proc nil [{:eventName "another.event"}])
      => [nil [{:eventName "another.event"}]]
      )



(fact "if the events is a started followed by a stop then it should
       generate a new event for the same device with the same gate
       name but changed into done, with an additional duration
       attribute and few more attributes"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}
            {:eventName "game.play.done"    :timestamp 1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9 }]
        )
      )



(fact "When the new event is generated the extra attributes are
       merged as well giving a priority to the stop
       event (overwrite save attributes in the start event)"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1" :level 1 :points 650}
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1" :points 23456 :end-level 10}])]
        events
        => [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1" :level 1 :points 650}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1" :points 23456 :end-level 10}
            {:eventName "game.play.done"    :timestamp 1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9
             :level 1 :end-level 10 :points 23456}]
        )

      )



(fact "Two started events followed by one stop, the stop
       will match the last start"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
                   {:eventName "game.play.started" :timestamp 5  :sourceId "dev1"} ;; this one should be matched
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
            {:eventName "game.play.started" :timestamp 5  :sourceId "dev1"}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}
            {:eventName "game.play.done"    :timestamp 5  :sourceId "dev1"
             :inferred true :startTs 5 :stopTs 10 :duration 5}]
        )

      )



(fact "A stop without a start should just be ignored"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.stopped" :timestamp 1  :sourceId "dev1"}
                   {:eventName "game.play.started" :timestamp 5  :sourceId "dev1"}
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.stopped" :timestamp 1  :sourceId "dev1"}
            {:eventName "game.play.started" :timestamp 5  :sourceId "dev1"}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}
            {:eventName "game.play.done"    :timestamp 5  :sourceId "dev1"
             :inferred true :startTs 5 :stopTs 10 :duration 5}]
        )

      )



(fact "if IDs are present then should appear int the merged event as well"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started" :timestamp 5  :sourceId "dev1" :id "id1"}
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1" :id "id2"}])]
        events
        => [{:eventName "game.play.started" :timestamp 5  :sourceId "dev1" :id "id1"}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1" :id "id2"}
            {:eventName "game.play.done"    :timestamp 5  :sourceId "dev1"
             :inferred true :startTs 5 :stopTs 10 :duration 5
             :startEventId "id1" :stopEventId "id2"}]
        )

      )



(fact "different type of start/stop pairs shouldn't mix up"

      ;; partially overlapping
      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started"  :timestamp  1  :sourceId "dev1"}
                   {:eventName "game.music.started" :timestamp  5  :sourceId "dev1"}
                   {:eventName "game.play.stopped"  :timestamp 10  :sourceId "dev1"}
                   {:eventName "game.music.stopped" :timestamp 15  :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started"  :timestamp   1  :sourceId "dev1"}
            {:eventName "game.music.started" :timestamp   5  :sourceId "dev1"}
            {:eventName "game.play.stopped"  :timestamp  10  :sourceId "dev1"}
            {:eventName "game.play.done"     :timestamp   1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9}
            {:eventName "game.music.stopped" :timestamp  15  :sourceId "dev1"}
            {:eventName "game.music.done"    :timestamp   5  :sourceId "dev1"
             :inferred true :startTs 5 :stopTs 15 :duration 10}]

        )



      ;; fully overlapping
      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started"  :timestamp  1  :sourceId "dev1"}
                   {:eventName "game.music.started" :timestamp  5  :sourceId "dev1"}
                   {:eventName "game.music.stopped" :timestamp  8  :sourceId "dev1"}
                   {:eventName "game.play.stopped"  :timestamp 10  :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started"  :timestamp   1  :sourceId "dev1"}
            {:eventName "game.music.started" :timestamp   5  :sourceId "dev1"}
            {:eventName "game.music.stopped" :timestamp   8  :sourceId "dev1"}
            {:eventName "game.music.done"    :timestamp   5  :sourceId "dev1"
             :inferred true :startTs 5 :stopTs 8 :duration 3}
            {:eventName "game.play.stopped"  :timestamp  10  :sourceId "dev1"}
            {:eventName "game.play.done"     :timestamp   1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9}
            ]

        )
      )



(fact "Consecutive start/stop for the same sourceId and same type should mix up"

      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
                   {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}
                   {:eventName "game.play.started" :timestamp 11 :sourceId "dev1"}
                   {:eventName "game.play.stopped" :timestamp 20 :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started" :timestamp 1  :sourceId "dev1"}
            {:eventName "game.play.stopped" :timestamp 10 :sourceId "dev1"}
            {:eventName "game.play.done"    :timestamp 1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9 }
            {:eventName "game.play.started" :timestamp 11 :sourceId "dev1"}
            {:eventName "game.play.stopped" :timestamp 20 :sourceId "dev1"}
            {:eventName "game.play.done"    :timestamp 11 :sourceId "dev1"
             :inferred true :startTs 11 :stopTs 20 :duration 9 }]
        )
      )



(fact "different sourceId start/stop pairs shouldn't mix up"

      ;; partially overlapping
      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started"  :timestamp  1  :sourceId "dev1"}
                   {:eventName "game.play.started"  :timestamp  5  :sourceId "dev2"}
                   {:eventName "game.play.stopped"  :timestamp 10  :sourceId "dev1"}
                   {:eventName "game.play.stopped"  :timestamp 15  :sourceId "dev2"}])]
        events
        => [{:eventName "game.play.started"  :timestamp   1  :sourceId "dev1"}
            {:eventName "game.play.started"  :timestamp   5  :sourceId "dev2"}
            {:eventName "game.play.stopped"  :timestamp  10  :sourceId "dev1"}
            {:eventName "game.play.done"     :timestamp   1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9}
            {:eventName "game.play.stopped" :timestamp  15  :sourceId "dev2"}
            {:eventName "game.play.done"    :timestamp   5  :sourceId "dev2"
             :inferred true :startTs 5 :stopTs 15 :duration 10}]

        )



      ;; fully overlapping
      (let [state0 (kv/make-in-memory-kvstore)
            [state events]
            (proc state0
                  [{:eventName "game.play.started"  :timestamp  1  :sourceId "dev1"}
                   {:eventName "game.play.started"  :timestamp  5  :sourceId "dev2"}
                   {:eventName "game.play.stopped"  :timestamp  8  :sourceId "dev2"}
                   {:eventName "game.play.stopped"  :timestamp 10  :sourceId "dev1"}])]
        events
        => [{:eventName "game.play.started" :timestamp   1  :sourceId "dev1"}
            {:eventName "game.play.started" :timestamp   5  :sourceId "dev2"}
            {:eventName "game.play.stopped" :timestamp   8  :sourceId "dev2"}
            {:eventName "game.play.done"    :timestamp   5  :sourceId "dev2"
             :inferred true :startTs 5 :stopTs 8 :duration 3}
            {:eventName "game.play.stopped"  :timestamp  10  :sourceId "dev1"}
            {:eventName "game.play.done"     :timestamp   1  :sourceId "dev1"
             :inferred true :startTs 1 :stopTs 10 :duration 9}
            ]

        )
      )
