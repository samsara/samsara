(ns moebius.core
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log]))



(defn- stepper [f]
  (fn [{:keys [to-process processed] :as domain}]
    (if-not (seq to-process)
      domain
      (let [[head & tail] (f (first to-process))]
        {:to-process (seq (concat tail (next to-process)))
         :processed  (conj processed head)}))))


(defn cycler [f events]
  (->> (stepper f)
       (#(iterate % {:to-process events :processed []}))
       (drop-while :to-process)
       first
       :processed))


(defn enricher [f]
  (fn [event]
    [(f event)]))

(defn correlator [f]
  (fn [event]
    (f event)))


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

      (when-event-is event \"device.booted\"
          (-> event
              (assoc :new-property \"a-value\")
              (assoc :property2 6)))


   alternatively you can provide a list of event's names:

      (when-event-is event [\"device.booted\" \"device.activated\"]
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
  (assoc e :even? (even? a)))

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


(cycler f evs )
