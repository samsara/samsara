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


(comment

  (def c (component/start
          (samsara-client
           {:url "http://localhost:9000"
            :max-buffer-size 3
            :publish-interval 3000})))

  (def k (record-event! c {:eventName "a" :timestamp 2 :sourceId "d1"}))

  (count @(:buffer c))

  (component/stop c)

  )



(comment

  (init! {:url "http://localhost:9000"
          :max-buffer-size 3
          :publish-interval 3000})

  (def k1 (record-event! {:eventName "a" :timestamp 2 :sourceId "d1"}))

  (count @(:buffer *samsara-client*))

  (stop!)

  )
