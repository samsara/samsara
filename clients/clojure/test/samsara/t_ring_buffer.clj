(ns samsara.t-ring-buffer
  (:use midje.sweet)
  (:require [samsara.ring-buffer :refer :all]
            [midje.sweet :refer :all]))

(def ^{:dynamic true} buffer (atom (ring-buffer 5)))

(defmacro with-empty-buffer [& body]
  `(binding [buffer (atom (ring-buffer 5))]
     ~@body))

(defn enqueue-item
  [item]
  "Atomically adds an item to the ring buffer"
  (swap! buffer enqueue item))

(defn enqueue-items
  [items]
  "Atomically adds a list of items to the ring buffer"
  (doall (map enqueue-item items)))


(fact "enqueue function adds items to the ring buffer. Items are added as key value pairs with a unique id (auto incrementing number). (snapshot buffer) returns the current state of the ring buffer. (items) returns just the items without the id.  "
      (with-empty-buffer
        (enqueue-item "One")
        (snapshot @buffer)  => (list [1 "One"])
        (items @buffer) => (list "One")))

(fact "enqueue function overwrites the oldest item if the buffer reaches capacity"
      (with-empty-buffer
        (enqueue-items ["One" "Two" "Three" "Four" "Five" "Six"])
        (snapshot @buffer) => (list [2 "Two"] [3 "Three"] [4 "Four"] [5 "Five"] [6 "Six"])))

(fact "dequeue removes items in the buffer with the corresponding ids if they exisit in the buffer"
      (with-empty-buffer
        (enqueue-items ["One" "Two" "Three" "Four" "Five"])
        (let [clip (snapshot @buffer)]
          ;; Test if snapshot returned all the items in the buffer
          clip => (list [1 "One"] [2 "Two"] [3 "Three"] [4 "Four"] [5 "Five"])
          ;; Add another item to the buffer. This should overwrite the oldest.ie "One"
          (enqueue-item "Six")
          ;; Test if "One" is no longer in the snapshot.
          (snapshot @buffer) => (list [2 "Two"] [3 "Three"] [4 "Four"] [5 "Five"] [6 "Six"])
          ;; dequeue using the first snapshot obtained.
          (swap! buffer dequeue clip)
          ;; Test if dequeue removed all items in the original snapshot
          ;; and retains "Six"
          (snapshot @buffer) => (list [6 "Six"]))
        ))
