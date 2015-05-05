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



(defn enricher
  "Takes a function which accepts an event and wraps the result
  into an array as expected by the `cycler`"
  [f]
  (fn [event]
    [(f event)]))



(defn correlator
  "Takes a function which accept an event and turns the output into
  something expected by the cycler"
  [f]
  (fn [event]
    (let [r (f event)]
      (cons event r))))



(defn filterer
  "Similar to `filter` it takes a predicate which applied to
  an event return something truthy for the events to keep."
  [pred]
  (fn [event]
    [(when (pred event) event)]))



(defn pipeline
  "Pipeline composese stearming processing functions
  into a chain of processing which is then applied by
  `cycler`"
  [& fs]
  (->> fs
       (map (fn [f]
              (fn [[head & tail :as evs]]
                (if head
                  (concat (f head) tail)
                  evs))))
       reverse
       (apply comp)
       (#(comp % (fn [e] [e])))))



(defmacro defenrich
  "handy macro to define an enrichment function"
  [name params & body]
  `(def ~name
     (enricher
      (fn ~params
        ~@body))))



(defmacro defcorrelate
  "handy macro to define an correlation function"
  [name params & body]
  `(def ~name
     (correlator
      (fn ~params
        ~@body))))



(defmacro deffilter
  "handy macro to define an filter function"
  [name params & body]
  `(def ~name
     (filterer
      (fn ~params
        ~@body))))



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



(defn moebius
  "It takes a list of functions transformation and produces a function
   which applied to a sequence of events will apply those transformations."
  [& fs]
  (partial cycler (apply pipeline fs)))
