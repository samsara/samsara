(ns user
  (:require [samsara.client :refer :all]))


(comment

  ;; validate single event
  (validate-events :single
                   {:eventName "a" :timestamp 1 :sourceId "a"})


  ;; validate batch of events
  (validate-events :batch
                   [{:eventName "a" :timestamp 1 :sourceId "a"}
                    {:eventName "b" :timestamp 2 :sourceId "b"}])


  ;; publish a bunch of events
  (publish-events "http://localhost:9000/v1"
                  [{:eventName "a" :timestamp 1 :sourceId "a"}
                   {:eventName "b" :timestamp 2 :sourceId "b"}])


  )
