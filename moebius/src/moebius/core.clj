(ns moebius.core
  (:require [clojure.string :as s])
  (:require [clojure.java.io :as io])
  (:require [taoensso.timbre :as log])
  (:require [clojure.core.match :refer [match]]))


;; TODO:
;; filter followed by enrichment seems to enrich a nil
;;


(defn- stateful
  "Wraps a stateless function into a stateful one.
  It returns a function which takes two parameters `state` and `event`
  and applies `f` to the event and leave the state unchanged."
  [f]
  (fn [state event]
    [state (f event)]))



(defn- stateful-pred
  "Wraps a stateless predicate function into a stateful one.
  It returns a function which takes two parameters `state` and `event`
  and applies `pred` to the event and ignore the state."
  [pred]
  (fn [state event]
    (pred event)))



(defn- stepper
  "It returns a function which performs a single step in the cycle. It
  process the first event from the `to-process` and put the result in
  `processed` if not `nil`. When the result is expanded with multiple
  events then these are added to head of `to-process`."
  [f]
  (fn [{:keys [to-process processed state] :as domain}]
    (if-not (seq to-process)
      domain
      (let [[new-state [head & tail]] (f state (first to-process))]
        {:to-process (seq (concat tail (next to-process)))
         :state      new-state
         :processed  (if head (conj processed head) processed)}))))



(defn- cycler
  "It takes a functional and a list of events.
  The function `f` must return a list of 0, 1 or more elements.  When
  the `f` returns an empty list, the event it has been filtered
  out. When the function `f` returns only 1 element it is commonly
  referred as enrichment process. When the `f` returns more than one
  elements the first one is typically the event which has been
  processed and any additional are correlated events which will be put
  back in the cycle to to follow the same process. If you whish to
  just `expand` and event (given an event produce 2 or more events and
  discard the original event) then all you need to do is return a list
  of events in which the first element is `nil`.  This is called
  expansion.
  "
  [f state events]
  (->> (stepper f)
       (#(iterate % {:to-process events :state state :processed []}))
       (drop-while :to-process)
       first
       ((juxt :state :processed))))



(defn moebius-fn
  "Takes a normal clojure function and add the necessary metadata
   to be used inside a pipeline"
  [name type statefulness f]
  (with-meta f
    {:moebius-name name
     :moebius-wrapper statefulness
     :moebius-type type}))


(defmacro -def-moebius-function
  "handy macro to define an moebius function"
  [type fname params & body]
  (let [p# (count params)
        wrapper (if (= 2 p#) :stateful :stateless)]
    (if (not (<= 1 p# 2))
      (throw (IllegalArgumentException.
              (str "Invalid number of parameters for function: "
                   name ". It can be either [event] or [state event]")))
      `(def ~fname
         (moebius-fn (str '~fname) ~type ~wrapper
           (fn ~params
             ~@body))))))


;; (defn enricher
;;   "Takes a function which accepts an event and wraps the result
;;   into an array as expected by the `cycler`"
;;   [f]
;;   (fn [event]
;;     (let [result (f event)]
;;       (if result
;;         [result]
;;         [event]))))

;; TODO: should return state or s' when returned state is nil?
(defn- enricher
  "Takes a function which accepts an event and wraps the result
  into an array as expected by the `cycler`"
  [f]
  (fn [[state [event & tail]]]
    (let [[s' e'] (f state event)]
      (if e'
        [s' (concat [e'] tail)]
        [s' [event]]))))





((stateful #(assoc % :b 3)) 1 {:a 1})

((enricher #(vector (inc %) (assoc %2 :b 3))) [1 [{:a 1}]])

((enricher (stateful #(assoc % :b 3))) [1 [{:a 1}]])




;; (defn correlator
;;   "Takes a function which accept an event and turns the output into
;;   something expected by the cycler"
;;   [f]
;;   (fn [event]
;;     (let [r  (f event)
;;           rn (if (map? r) [r] r)]
;;       (cons event rn))))



(defn- correlator
  "Takes a function which accept an event and turns the output into
  something expected by the cycler"
  [f]
  (fn [[state [event & tail]]]
    (let [[s' r]  (f state event)
          rn (if (map? r) [r] r)]
      [s' (concat [event] rn tail)])))



;; (defn filterer
;;   "Similar to `filter` it takes a predicate which applied to
;;   an event return something truthy for the events to keep."
;;   [pred]
;;   (fn [event]
;;     [(when (pred event) event)]))



(defn- filterer
  "Similar to `filter` it takes a predicate which applied to
  an event return something truthy for the events to keep."
  [pred]
  (fn [[state [event & tail]]]
    [state (concat [(when (pred state event) event)] tail)]))

((filterer (fn [s e] (when-not (:z e)))) [1 [{:a 1}]])

;; (defn pipeline
;;   "Pipeline composes stearming processing functions
;;   into a chain of processing which is then applied by
;;   `cycler`"
;;   [& fs]
;;   (->> fs
;;        (map (fn [f]
;;               (fn [[head & tail :as evs]]
;;                 (if head
;;                   (concat (f head) tail)
;;                   evs))))
;;        reverse
;;        (apply comp)
;;        (#(comp % (fn [e] [e])))))


(defn- as-stateful [f {:keys [moebius-wrapper moebius-type] :as m}]
  (let [stateful' (if (= :filtering moebius-type) stateful-pred stateful)
        wrapped (case moebius-wrapper :stateful f :stateless (stateful' f))]
    wrapped))

(defn- wrapped-fn [f {:keys [moebius-wrapper moebius-type] :as m}]
  (let [wrapper (case moebius-type
                  :enrichment enricher
                  :correlation correlator
                  :filtering filterer)]
    (with-meta (wrapper f) (assoc m :moebius-normalized true))))


;; TODO: collect and join all meta
(defn pipeline
  "Pipeline composes stearming processing functions
  into a chain of processing which is then applied by
  `cycler`"
  [& fs]
  (->> fs
       (map (fn [f]
              (let [m (meta f)]
                (wrapped-fn (as-stateful f m) m))))
       reverse
       (apply comp)
       (#(comp % (fn [state e] [state [e]])))
       ))

;((stateless #(assoc % :b 3)) 1 {:a 1})

;((enricher #(vector (inc %) (assoc %2 :b 3))) [1 [{:a 1}]])

;((enricher (stateless #(assoc % :b 3))) [1 {:a 1}])


(def x (pipeline (with-meta #(assoc % :b 2) {:moebius-wrapper :stateless
                                             :moebius-type    :enrichment})
                 (with-meta #(assoc % :c 3) {:moebius-wrapper :stateless
                                             :moebius-type    :enrichment})
                 (with-meta (fn [s e] [(inc s) (when-not (:x e) [(assoc e :x 2) (assoc e :x 3)])]) {:moebius-wrapper :stateful
                                                                                                   :moebius-type    :correlation})
                 (with-meta #(vector (inc %) (assoc %2 :z 3))
                   {:moebius-wrapper :stateful
                    :moebius-type    :enrichment})
                 (with-meta (fn [s e] (when-not (:z e)))
                   {:moebius-wrapper :stateful
                    :moebius-type    :filtering})
                 (with-meta #(vector (inc %) (assoc %2 :y 5))
                   {:moebius-wrapper :stateful
                    :moebius-type    :enrichment})))

(x 1 {:a 1})

;; (defmacro defenrich
;;   "handy macro to define an enrichment function"
;;   [name params & body]
;;   `(def ~name
;;      (enricher
;;       (fn ~params
;;         ~@body))))



;; (defmacro defcorrelate
;;   "handy macro to define an correlation function"
;;   [name params & body]
;;   `(def ~name
;;      (correlator
;;       (fn ~params
;;         ~@body))))



;; (defmacro deffilter
;;   "handy macro to define an filter function"
;;   [name params & body]
;;   `(def ~name
;;      (filterer
;;       (fn ~params
;;         ~@body))))



(defmacro defenrich
  "handy macro to define an enrichment function"
  [name params & body]
  `(-def-moebius-function :enrichment ~name ~params ~@body))



(defmacro defcorrelate
  "handy macro to define an correlations function"
  [name params & body]
  `(-def-moebius-function :correlation ~name ~params ~@body))



(defmacro deffilter
  "handy macro to define an filtering function"
  [name params & body]
  `(-def-moebius-function :filtering ~name ~params ~@body))



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



(defmacro when-event-name-is
  "if the eventName of the given event is equal to the give name
  then evaluate the body. Otherwhise `nil` is returned.

   example:

      (when-event-name-is event \"game.started\"
          (-> event
              (assoc :new-property \"a-value\")
              (assoc :property2 6)))


  alternatively you can provide a list of event's names:

      (when-event-name-is event [\"game.started\" \"game.level.completed\"]
          (assoc event :new-property \"a-value\"))

  "
  [event name & body]
  `(let [_event# ~event _name# ~name
         _names# (if (string? _name#) [_name#] _name#)]
     (when (contains? (set _names#) (:eventName _event#))
       ~@body)))



(defmacro when-event-match
  "If the event matches one of the patterns the related expression is
  evaluated and returned.
  If none matches `nil` is returned.

   example:


       (let [event {:eventName \"game.started\" :level 8}]
          (when-event-match event
            [{:eventName \"game.started\" :level 0}]               (assoc event :new-player true)
            [{:eventName _ :level (_ :guard even?)}]             (assoc event :start :even-level)
            [{:eventName _ :level (_ :guard #(= 0 (mod % 11)))}] (assoc event :level-type :extra-challenge)
            [{:eventName \"game.new.level\" :level _}]             (assoc event :level-type :normal)))

  It implies a `:else` statement so you can't use one in yours.

  "
  [event & body]
  `(let [_event# ~event]
     (match [_event#]
            ~@body
            :else nil)))



(def
  ^{:doc
    "Given a glob pattern it compiles it down to a regular expression
     which can be used with functions like `re-matches`, `re-find`, etc.
     To improve performances the function is `memoized` so common
     patterns are compiled only once."
    :arglists '([glob])}
  glob-pattern
  (memoize
   (fn [glob]
     (re-pattern
      (-> (str "^" glob "$")
          (s/replace "." "\\.")
          (s/replace "**" "[[::multi::]]")
          (s/replace "*"  "[[::single::]]")
          (s/replace "[[::multi::]]"  ".*")
          (s/replace "[[::single::]]" "[^.]*"))))))



(defn match-glob
  "Glob matching simplifies the event matching when names
  are in a dotted form. Allowed globs are:

  Dotted forms name are as follow:

    <segment>.<segment>.<...>.<segment>

    *  - single * matches any single segment
    ** - matches multiple segments

  For example:

    (match-glob \"game.*.started\"  \"game.level.started\")   => truthy
    (match-glob \"game.*.started\"  \"game.level.2.started\") => falsey
    (match-glob \"game.**.started\" \"game.level.2.started\") => truthy
    (match-glob \"game.**\"         \"game.level.5.stopped\") => truthy
    (match-glob \"game.**\"         \"game.anything.else\")   => truthy
    (match-glob \"game.**.ended\"   \"game.1.2.3.ended\")     => truthy

  "
  [glob name]
  (re-matches (glob-pattern glob) name))



(defn moebius
  "It takes a list of functions transformation and produces a function
   which applied to a sequence of events will apply those transformations."
  [& fs]
  (partial cycler (apply pipeline fs)))
