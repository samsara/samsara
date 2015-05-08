(ns samsara.ring-buffer
  (:require [amalloy.ring-buffer :as rb]
            [taoensso.timbre :as log]))


(defn- pop-while
  "like drop-while but for amalloy/ring-buffer"
  [pred buffer]
  (if (pred (peek buffer))
    (recur pred (pop buffer))
    buffer))


(defn- drop-items
  "Given a list of [id items] will remove the items
  which are still in the amalloy/ring-buffer."
  [buffer items]
  (let [to-remove (set (map first items))]
    (pop-while (comp to-remove first) buffer)))


(defprotocol PRingBuffer
  "Protocol that the RingBuffer type must implement."
  (enqueue! [this item]
    "Add an item to the Ring Buffer.")
  (dequeue! [this items]
    "Given a list of [id events] will remove the events which are still in the Ring Buffer.")
  (snapshot [this]
    "Returns an immutable snapshot of the current state of the Ring Buffer.")
  (items [this]
    "Returns a list containing just the items in the buffer.")
  )

(deftype RingBuffer [^:clojure.lang.Atom !counter! ^:clojure.lang.Atom !buffer!]
  ;;Ring Buffer Implementation for Samsara Client. This type uses amalloy/ring-buffer.
  ;;Every item is assigned an unique id and added as a pair like [1 {:eventName...].
  ;;This id will be used to remove the items which have been successfully processed.
  ;;This type currently does not support (pop n) type operations. The snapshot function
  ;;will return the current snapshot as-is. This wont help in scenarios where more than
  ;;one consumer is involved.

  Object
  (toString [this]
    (pr-str (deref !buffer!)))

  PRingBuffer
  (enqueue! [this item]
    (let [id (swap! !counter! inc)]
      (swap! !buffer! into [[id item]])))
  (snapshot [this]
    (deref !buffer!))
  (items [this]
    (map second (deref !buffer!)))
  (dequeue! [this items]
    (swap! !buffer! drop-items items)
    )
  )

(defn ring-buffer
  "Create an empty ring buffer with the specified [capacity]."
  [capacity]
  (RingBuffer. (atom 0) (atom (rb/ring-buffer capacity)) ) )
