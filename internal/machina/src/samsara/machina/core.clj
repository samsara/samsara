(ns samsara.machina.core
  (:refer-clojure :exclude [error-handler])
  (:require [samsara.machina.wrappers :refer :all]
            [safely.core :as safely]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;      ---==| D E F A U L T   S T A T E   T R A N S I T I O N S |==----      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn halt-machina
  "it takes a machine and a reason for which should be halted
   and returns an new state machine in halted state"
  [{:keys [state] :as sm} reason]
  (assoc sm
         :state :machina/halted
         :machina/halted {:reason     reason
                          :form-state state}))



(defn sleep-transition
  "When the machine is a `:machina/sleep` state then this transition
   will sleep for a while and the go the return state.

       ;; it transition form the sleep state
       {:state :machina/sleep
        :machina/sleep
        {;; nap it can be the number of milliseconds to sleep
         ;; :nap 3000
         ;; alternatively nap can be a safely/retry-delay spec
         ;; see: https://github.com/BrunoBonacci/safely
         :nap [:random 3000 :+/- 0.35]
         ;; finally it can be a function
         ;; which sleeps for a while.
         ;; :nap (fn [] (Thread/sleep 2500))
         ;; typically used in conjunction with `safely/sleep`
         ;;
         ;; finally the sleeper state needs to know which
         ;; state does it needs to go after the little nap.
         :return-state :foo}}

  After the nap it will set the state to `:return-state`
  and remove the `:machina/sleep` key.
  "
  [{{:keys [nap return-state]} :machina/sleep :as sm}]
  (if-not (and (or (fn? nap) (vector? nap) (number? nap)) return-state)
    (halt-machina sm "Invalid :machina/sleep state.")

    (do ;; a little nap
      (cond
        (fn?     nap) (nap)
        (vector? nap) (apply safely/sleeper nap)
        (number? nap) (safely/sleep nap))

      ;; set next state to the return-state
      (-> sm
          (assoc  :state  return-state)
          (dissoc :machina/sleep)))))



(defmulti error-policy
  "returns a function which operates on the state machine
  and handles the error according to the given policy"
  (fn [policy origin-state] (:type policy)))



(defmethod error-policy :default
  [{:keys [retry-delay max-retry] :as policy} retry-state]
  (fn [sm]
    (halt-machina sm (str "Invalid error policy for state: " retry-state))))


;;
;; TODO: handle max-retry != :forever
;;
(defmethod error-policy :retry
  [{:keys [retry-delay max-retry] :as policy} retry-state]
  (fn [sm]
    (let [;; if a nap function already exists (from previous errors)
          ;; the use it, otherwise create a new one
          nap (or (get-in sm [:machina/latest-errors retry-state :sleeper])
                 (apply safely/sleeper retry-delay))]
      (-> sm
          ;; save the nap function in the latest-errors
          (assoc-in [:machina/latest-errors retry-state :sleeper] nap)
          ;; prepare for the sleep state
          (assoc :machina/sleep {:nap nap :return-state retry-state})
          ;; set the next state
          (assoc :state   :machina/sleep)))))



(defn error-transition
  "Make the transition from `:machina/error` to the managed target state
  it uses the defined `:machina/error-policies` and the error's information
  from `:machina/latest-errors` to decide how to act and which state to
  go next. The machina `:machina/latest-errors` is updated by
  the `error-handler` wrapper.

      {:state :machina/error
       ;; latest-errors keeps a map of the latest failures
       ;; by state.
       :machina/latest-errors
       {;; `:machina/from-state` contains which state causes the transition
        ;; to the error state
        :machina/from-state :foo,
        ;; here there is a from state -> last error
        :foo {;; for every error we keep a counter of how many consecutive
              ;; of error transition we have in this state
              :repeated 2,
              ;; last state-machine epoch error (required `epoch-counter` wrapper)
              :error-epoch 24,
              ;; optionally may contain a sleeper function which is used
              ;; to prepare the next sleep state and sleep of the appropriate
              ;; amount of time (ex: in exponential back off)
              ;; :sleeper (fn [] ,,,)
              ;; and the actual error
              :error Exception}}

       ;; the error policies controls how the un-handled errors are managed
       ;; this is a map from state -> error policy. The error policy
       ;; can be one of the predefined policies (see: doc TODO:link),
       ;; it can be a keyword representing a state where to go,
       ;; or it can be a function which take the state machine
       ;; and return an updated state machine.
       :machina/error-policies
       {;; the `:machina/default` defines the error policy to use
        ;; when a state specific error policy is not found.
        ;; If even the default error handler is not defined
        ;; the machine will transition to a halted state.
        :machina/default
        {:type         :retry
         :max-retry    :forever
         :retry-delay [:random-exp-backoff :base 200 :+/- 0.35 :max 60000]}

        ;; additionally you can specify for every other state in you SM
        ;; what to do in case of error:
        ;; for example the next policy retries every 5sec with a random
        ;; variant of +/- 35%.
        :foo   {:type        :retry
                :max-retry   :forever
                :retry-delay [:random 5000 :+/- 0.35]}

        ;; in this case, in case of error in the state `:bar` we go
        ;; directly to the state `:baz`
        :bar   :baz

        ;; finally you can run a function which will return a
        ;; a new state machine in a new state.
        :bax   (fn [sm] (assoc sm :state :machine/stop))}
  }


  For more info on available policies please go to the following page:
  TODO: doc link.
  "
  [{{:keys [machina/from-state]} :machina/latest-errors
    error-policies :machina/error-policies :as sm}]
  (let [policy (or (get error-policies from-state)
                  (get error-policies :machina/default))]

    (cond
      (nil?     policy) (halt-machina sm (str "State " from-state "raised an un-handled error."))
      (keyword? policy) (assoc sm :state policy)
      (fn?      policy) (policy sm)
      (map?     policy) ((error-policy policy from-state) sm)
      :else
      (halt-machina sm (str "Invalid error policy for state: " from-state)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| H E L P E R   F U N C T I O N S |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: doc
(defn with-dispatch
  ""
  {:style/indent 1}
  [sm state f]
  (assoc-in sm [:machina/dispatch state] f))


(defn move-to
  ;;TODO: doc
  ([state]
   (fn [sm] (assoc sm :state state)))
  ([sm state]
   (assoc sm :state state)))



(defn move-if
  ([pred? then-state else-state]
   (fn [sm]
     (if (pred? sm )
       (move-to sm then-state)
       (move-to sm else-state))))
  ([sm pred? then-state else-state]
   (if (pred? sm )
     (move-to sm then-state)
     (move-to sm else-state))))



(defn simple-machina
  "it return a simplified representation of the state machine
   which doesn't contain all internal keys and displays the
   remaining keys in alphabetic order with the `:state` as
   first key."
  [{:keys [state] :as sm}]
  (->> (dissoc sm :state)
       (remove (comp #(= "machina" (namespace %)) first))
       (sort-by first)
       (concat [[:state state]])
       (into (array-map))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                        ---==| M A C H I N A |==----                        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn default-machina
  []
  {:state :machina/start
   :machina/epoch 0
   :data  nil

   :machina/dispatch
   {:machina/stop   ^:no-wrap (fn [sm] sm)
    :machina/halted ^:no-wrap (fn [sm] (throw (ex-info "Machina is halted." sm)))
    :machina/sleep  sleep-transition
    :machina/error  error-transition}

   :machina/error-policies
   {:machina/default
    {:type        :retry
     :max-retry   :forever
     :retry-delay [:random-exp-backoff :base 200 :+/- 0.35 :max 60000]}
    }
   :machina/wrappers
   [#'epoch-counter #'log-state-change #'error-handler]})



(defn- reduce-wrappers
  ([] identity)
  ([f] f)
  ([f g] (g f))
  ([f g & fs]
   (reduce reduce-wrappers (concat [f g] fs))))



(defn transition
  [{:keys [state machina/dispatch machina/wrappers] :as sm}]
  (let [f0 (get dispatch state #(halt-machina % (str "Undefined state:" state)))
        f  (if (:no-wrap (meta f0))
             f0
             (apply reduce-wrappers (cons f0 (reverse wrappers))))]
    (f sm)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

  ;; write to file example
  (def sm
    (-> (default-machina)
        (update :machina/wrappers conj log-errors)
        (with-dispatch :machina/start
          (fn
            [sm]
            (assoc sm
                   :data "/tmp/1/2/3/4/5/file.txt"
                   :state :write-to-file)))

        (with-dispatch :write-to-file
          (fn
            [{f :data :as sm}]
            (write-to-file f)
            (assoc sm
                   :state :machina/sleep
                   :sleeper {:nap 1000 :return-state :write-to-file})))))



  (comment

    (-> sm
        transition
        transition
        transition
        #_transition
        #_transition
        simple-machina
        )

    (defn changing []
      (let [s (volatile! nil)]
        (fn [v]
          (let [continue (not= v @s)]
            (vswap! s (constantly v))
            continue))))

    (def x (changing))

    (->> sm
         (iterate transition)
         (take 20)
         #_(take-while #(not= :machina/stop (:state %)))
         (take-while changing)
         (last))


    ))


(comment

  (defn write-to-file [f]
    (spit f
          (str (java.util.Date.) \newline)
          :append true))

  )
