(ns user
  (:require [samsara.machina.core :refer :all]
            [clojure.tools.logging :as log]))

;;
;; # Basic concepts
;;
;; state machine is just a collection of functions
;; which take a state machine (sm) and return an updated
;; state machine
;;
;; f(sm) -> sm'
;;
;; The sm has a few conventions:
;;   - it is a map
;;   - it contains a key called `:state` which
;;     indicates the "current state".
;;   - the initial state is `:machina/start`
;;   - the final state is `:machina/stop`
;;   - by default, if you attempt to transition from `:machina/stop`
;;     you receive the same sm that you stated with.
;;     It is called the `identity` transition.
;;   - each state correspond to an action called `transition`
;;   - each transition is just a function form state to state
;;     f(sm) -> sm'
;;   - if the machine doesn't know how to transition to the next
;;   - state it goes to a `:machina/halted` state.
;;   - by default, if you attempt to transition from `:machina/halted`
;;     an exception will be thrown.
;;
;; Transitions are stored within the state machine
;; as a map that goes from state to dispatching function
;; which need to be executed in that state.
;; The key where all dispatching functions are configured
;; is called `:machina/dispatch`
;; If the above conventions are respected then you can use
;; a set of common functions and transitions which greatly
;; simplify your work.
;; The machina's way to transition from state to state
;; is by using a function called `transition`.
;; `transition` makes a single transition from source
;; state to destination state.

;;
;; # First steps
;;
;; the simplest state machine is a sm that goes from
;; `:machina/start` to `:machina/stop` this can be
;; represented as the following sm:

(def sm
  {:state :machina/start
   :machina/dispatch
   {:machina/start (fn [sm] (assoc sm :state :machina/stop))}})


(transition sm)

;;
;; simpler dispatch function with helper function
;;

;; same as before
(def sm
  (-> {:state :machina/start}
      (with-dispatch :machina/start
        (fn [sm] (assoc sm :state :machina/stop)))))


;; additionally if you just want move to another state
;; or as the final step of a dispatch function you can
;; use another helper function called `move-to`

(def sm
  (-> {:state :machina/start}
      (with-dispatch :machina/start
        (move-to :machina/stop))))

(transition sm)


;;
;; # Simple counter
;;

(def sm
  (-> {:state :machina/start}

      ;; :start -> :count-down
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :counter] 5)
              (move-to :count-down))))

      ;; :count-down -> :count-down/:stop
      (with-dispatch :count-down
        (fn [sm]
          (as-> sm $
            (update-in $ [:data :counter] dec)
            (if (> (get-in $ [:data :counter]) 0)
              (move-to $ :count-down)
              (move-to $ :machina/stop)))))))


(transition sm)

(-> sm
    transition
    transition
    transition)


(iterate transition sm)

(->> sm
     (iterate transition)
     (take-while #(not= :machina/stop (:state %)))
     (map simple-machina))


;;
;; move-if
;;
(def sm
  (-> {:state :machina/start}

      ;; :start -> :count-down
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :counter] 5)
              (move-to :count-down))))

      ;; :count-down -> :count-down/:stop
      (with-dispatch :count-down
        (fn [sm]
          (-> sm
              (update-in [:data :counter] dec)
              (move-if #(> (-> % :data :counter) 0)
                       :count-down :machina/stop))))))


(->> sm
     (iterate transition)
     (take-while #(not= :machina/stop (:state %)))
     (map simple-machina))



;; lets' get the last state too.
(->> sm
     (iterate transition)
     (take-while (until-stopped))
     (map simple-machina))

;;
;; What if i wanted to display all the state transitions?
;; Once approach would be to add a `println` statement
;; on every transition. But there is a better way.
;; In machina we support wrappers. This are functions
;; that take an handler function in input and return
;; a function which handle the state-machine by
;; wrapping the input handler.
;;

(defn show-transitions [handler]
  (fn [{:keys [state] :as sm}]
    (let [sm1 (handler sm)]
      (println "transition: from:" state "-->" (:state sm1))
      sm1)))

(def sm
  (-> {:state :machina/start
       :machina/wrappers [#'show-transitions]}

      ;; :start -> :count-down
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :counter] 5)
              (move-to :count-down))))

      ;; :count-down -> :count-down/:stop
      (with-dispatch :count-down
        (fn [sm]
          (-> sm
              (update-in [:data :counter] dec)
              (move-if #(> (-> % :data :counter) 0)
                       :count-down :machina/stop))))))


(->> sm
     (iterate transition)
     (take-while (until-stopped))
     last
     simple-machina)



;;
;; Let see how to handle errors. In this example I want
;; to write to a file once a second when possible.
;; (aka: write folder is available)
;;

(defn write-to-file! [f]
  (spit f
        (str (java.util.Date.) \newline)
        :append true))


(def sm
  (-> {:state :machina/start
       :machina/wrappers [#'show-transitions]}

      ;; :start -> :write-to-file
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :file] "/tmp/3/2/1/file.txt")
              (move-to :write-to-file))))

      ;; :write-to-file -> :sleep
      (with-dispatch :write-to-file
        (fn
          [sm]
          (write-to-file! (-> sm :data :file))
          (-> sm
              (assoc-in [:data :sleep-time] 1000)
              (move-to :sleep))))

      ;; :sleep -> :write-to-file
      (with-dispatch :sleep
        (fn
          [sm]
          (Thread/sleep (get-in sm [:data :sleep-time]))
          (move-to sm :write-to-file)))))


;; exception thrown at the second transition
(-> sm
    transition
    transition
    )

;;
;; Manage errors transition-by-transition
;;
(def sm
  (-> {:state :machina/start
       :machina/wrappers [#'show-transitions]}

      ;; :start -> :write-to-file
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :file] "/tmp/3/2/1/file.txt")
              (move-to :write-to-file))))

      ;; :write-to-file -> :sleep
      (with-dispatch :write-to-file
        (fn
          [sm]
          ;; HERE: adding error handling logic
          (try
            (write-to-file! (-> sm :data :file))
            (catch Exception x
              (log/warn "got an error:" (.getMessage x))))
          (-> sm
              (assoc-in [:data :sleep-time] 1000)
              (move-to :sleep))))

      ;; :sleep -> :write-to-file
      (with-dispatch :sleep
        (fn
          [sm]
          (Thread/sleep (get-in sm [:data :sleep-time]))
          (move-to sm :write-to-file)))))

(-> sm
    transition
    transition
    transition
    )


;;
;; Manage errors with wrapper
;;

(-> (default-machina) :machina/wrappers)

(def sm
  (-> (default-machina)

      ;; :start -> :write-to-file
      (with-dispatch :machina/start
        (fn [sm]
          (-> sm
              (assoc-in [:data :file] "/tmp/3/2/1/file.txt")
              (move-to :write-to-file))))

      ;; :write-to-file -> :sleep
      (with-dispatch :write-to-file
        (fn
          [sm]
          ;; HERE: adding error handling logic
          (write-to-file! (-> sm :data :file))
          (-> sm
              (assoc-in [:data :sleep-time] 1000)
              (move-to :sleep))))

      ;; :sleep -> :write-to-file
      (with-dispatch :sleep
        (fn
          [sm]
          (Thread/sleep (get-in sm [:data :sleep-time]))
          (move-to sm :write-to-file)))))

(->> sm
     (iterate transition)
     (take 20)
     (map simple-machina)
     )

;; check default policies
(default-machina)

;; end


;;
;; Possible extensions:
;;
;; * graph? dynamic? static?
;; * global atom handling functions
;; * deftrans* style macros
