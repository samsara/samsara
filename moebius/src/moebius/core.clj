(ns moebius.core
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log]))



(defn- stepper
  "It returns a function which performs a single
  step in the cycle. It process the first event
  from the `to-process` and put the result in
  `processed` if not `nil`. When the result is
  expanded with multiple events then these are
  added to head of `to-process`."
  [f]
  (fn [{:keys [to-process processed] :as domain}]
    (if-not (seq to-process)
      domain
      (let [[head & tail] (f (first to-process))]
        {:to-process (seq (concat tail (next to-process)))
         :processed  (if head (conj processed head) processed)}))))


(defn cycler
  "It takes a functiona and a list of events.
  The function `f` must return a list of 0, 1 or more elements.
  When the `f` returns an empty list, the event it has been
  filtered out. When the function `f` returns only 1 element
  it is commonly referred as enrichment process. When the `f`
  returns more than one elements the first one is typically
  the event which has been processed and any addional are
  correlated events which will be put back in the cycle to
  to follow the same process. If you whish to just `expand` and
  event (given an event produce 2 or more events and discard
  the original event) then all you need to do is return a
  list of events in which the first element is `nil`.
  This is called expansion.
  "
  [f events]
  (->> (stepper f)
       (#(iterate % {:to-process events :processed []}))
       (drop-while :to-process)
       first
       :processed))



(defn enricher [f]
  (fn [event]
    (let[r (f event)]
      (if r [r] []))))


(defn correlator [f]
  (fn [event]
    (let [r (f event)]
      (or r []))))


(defn filterer [pred]
  (fn [event]
    (if (pred event) [event] [])))


(defn pipeline [& fs]
  (->> fs
       (map (fn [f]
              (fn [[head & tail]]
                (concat (f head) tail))))
       reverse
       (apply comp)
       (#(comp % (fn [e] [e])))))


(defmacro defenrich [name params & body]
  `(def ~name
     (enricher
      (fn ~params
        ~@body))))


(defmacro defcorrelate [name params & body]
  `(def ~name
     (correlator
      (fn ~params
        ~@body))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn inject-if
  "injects `value` if `condition` is truthy,
  to the given `event` with the `property` name"
  [event condition property value]
  (if condition
    (assoc event property value)
    event))


(defn inject-as
  "injects `value` if not null, to the given `event`
  with the `property` name"
  [event property value]
  (inject-if event value property value))


(defmacro when-event-is
  "if the eventName of the given event is equal to the give name
   then evaluate the body. nil otherwhise.

   example:

      (when-event-is event \"game.started\"
          (-> event
              (assoc :new-property \"a-value\")
              (assoc :property2 6)))


   alternatively you can provide a list of event's names:

      (when-event-is event [\"game.started\" \"game.level.completed\"]
          (assoc event :new-property \"a-value\"))

  "
  [event name & body]
  `(let [_event# ~event _name# ~name
         _names# (if (string? _name#) [_name#] _name#)]
     (if (contains? (set _names#) (:eventName _event#))
       ~@body
       _event#)))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def events
  [{:eventName "game.started" :timestamp 1 :sourceId "a" :level 1}
   {:eventName "game.level.completed" :timestamp 2 :sourceId "a" :levelCompleted 1}
   {:eventName "game.level.completed" :timestamp 3 :sourceId "a" :levelCompleted 2}
   {:eventName "game.level.completed" :timestamp 4 :sourceId "a" :levelCompleted 3}
   {:eventName "game.stopped" :timestamp 2 :sourceId "a" :level 4}])



(def evs [{:a 1} {:a 2} {:a 5}])


(defenrich ciao [e]
   (inject-as e :ciao :bello))

(defenrich even-odd? [{a :a :as e}]
  (inject-if e (even? a) :even? true))

(defn write-n [n]
  (fn [e]
    (assoc e :number n)))

(def write1 (enricher (write-n 1)))
(def write2 (enricher (write-n 2)))
(def write3 (enricher (write-n 3)))

(defcorrelate p1 [{a :a :as e}]
  (if (= a 2)
    [e {:a (inc a) } {:a (inc (inc a))}]
    [e]))

(def f (pipeline
        even-odd?
        write3
        p1
        write2
        write1
        ciao))


(cycler f evs)
