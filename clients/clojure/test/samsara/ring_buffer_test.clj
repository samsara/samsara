(ns samsara.ring-buffer-test
  (:require [samsara.ring-buffer :refer :all]
            [midje.sweet :refer :all]))



(fact "ring-buffer: create empty buffer has no items"

      (-> (ring-buffer 3)
          (items)) => '()

      )


(fact "ring-buffer: create and enqueue 1 item"

      (-> (ring-buffer 3)
          (enqueue :a)
          (items)) => '(:a)

      )


(fact "ring-buffer: create and enqueue items to fill the buffer"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (items)) => '(:a :b :c)

      )


(fact "ring-buffer: overrunning the buffer discards the oldest element"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (items)) => '(:b :c :d)


      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (enqueue :e)
          (enqueue :f)
          (items)) => '(:d :e :f)
      )


(fact "ring-buffer: create and enqueue 1 item and dequeue"

      (-> (ring-buffer 3)
          (enqueue :a)
          (snapshot)) => [[1 :a]]

      (-> (ring-buffer 3)
          (enqueue :a)
          (dequeue [[1 :a]])
          (items)) => '()

      )



(fact "ring-buffer: dequeue removes only matching ids"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :a)
          (snapshot)) => [[1 :a] [2 :a]]

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :a)
          (dequeue [[1 :a]])
          (snapshot)) => [[2 :a]]

      )


(fact "ring-buffer: dequeue with no matching ids is a no-op"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :a)
          (dequeue [[6 :a]])
          (snapshot)) => [[1 :a] [2 :a]]

      ;; no-op => before = after
      (let [r  (-> (ring-buffer 3)
                   (enqueue :a)
                   (enqueue :a))
            r1 (dequeue r [[6 :a]])]

        (= r r1))

      )


(fact "ring-buffer: dequeue with old ids after overrun is a no-op"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (enqueue :e)
          (enqueue :f)
          (dequeue [[1 :a] [2 :b]])
          (snapshot)) => [[4 :d] [5 :e] [6 :f]]
      )


(fact "ring-buffer: dequeue can work with present ids and overran ones"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (enqueue :e)
          (enqueue :f)
          (dequeue [[1 :a] [2 :b] [3 :c] [4 :d] [5 :e] [6 :f]])
          (snapshot)) => []
      )



(fact "ring-buffer: dequeue removes only consecutive ids"

      (-> (ring-buffer 3)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (enqueue :e) ;; you can't remove :f if you don't remove :e
          (enqueue :f)
          (dequeue [[1 :a] [2 :b] [3 :c] [4 :d] [6 :f]])
          (snapshot)) => [[5 :e] [6 :f]]
      )



(fact "ring-buffer: dequeue of one element in buffer of size 1"

      (-> (ring-buffer 1)
          (enqueue :a)
          (enqueue :b)
          (enqueue :c)
          (enqueue :d)
          (enqueue :e) ;; you can't remove :f if you don't remove :e
          (enqueue :f)
          (dequeue [[6 :f]])
          (snapshot)) => []
      )


(fact "ring-buffer: you can't create a buffer of size 0"

      (-> (ring-buffer 0)) => (throws Error)
      )
