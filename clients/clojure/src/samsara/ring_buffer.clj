(ns samsara.ring-buffer
  (:require [amalloy.ring-buffer :as rb]))


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

  (enqueue [this item]
           "Add an item to the Ring Buffer.")

  (dequeue [this items]
           "Given a list of [id events] will remove the events which are still in the Ring Buffer.")

  (snapshot [this]
            "Returns an immutable snapshot of the current state of the Ring Buffer.")

  (items [this]
         "Returns a list containing just the items in the buffer.")

  )


(deftype RingBuffer [counter buffer]
  ;; Ring Buffer Implementation for Samsara Client. This type uses
  ;; amalloy/ring-buffer.  Every item is assigned an unique id and
  ;; added as a pair like [1 {:eventName...}].  This id will be used to
  ;; remove the items which have been successfully processed.  This
  ;; type currently does not support (pop n) type operations. The
  ;; snapshot function will return the current snapshot as-is. This
  ;; wont help in scenarios where more than one consumer is involved.

  Object
  (toString [this]
    (pr-str buffer))

  PRingBuffer

  (enqueue [this item]
    (let [id (inc counter)]
      (RingBuffer. id (into buffer [[id item]]))))

  (snapshot [this] buffer)

  (items [this]
    (map second buffer))

  (dequeue [this items]
    (RingBuffer. counter (drop-items buffer items)))
  )


(defn ring-buffer
  "Create an empty ring buffer with the specified [capacity]."
  [capacity]
  (RingBuffer. 0 (rb/ring-buffer capacity)))
