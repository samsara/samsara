(ns samsara.machina.core
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
         ;; typically used in conjunction with `safely/sleeper`
         ;;
         ;; finally the sleeper state needs to know which
         ;; state does it needs to go after the little nap.
         :return-state :foo}}

  After the nap it will set the state to `:return-state`
  and remove the `:machina/sleeper` key.
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



(defn error-transition
  "Make the transition from `:machina/error` to the managed target state
  it uses the defined `:machina/error-policies` and the error's information
  from `:machina/latest-errors` to decide how to act and which state to
  go next. The machina `:machina/latest-errors` is updated by
  the `wrapper-error-handler`.

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
              ;; last state-machine epoch error (required `wrapper-epoch-counter`)
              :error-epoch 24,
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


  "
  [{{:keys [from-state]} :machina/latest-errors
    error-policies :machina/error-policies :as sm}]
  (if-let [policy (or (get error-policies from-state) (get error-policies :machina/default))]
    ;; TODO: multimethod dispatch by type?
    (let [{:keys [retry-delay]} policy
          nap (or (get-in sm [:latest-errors from-state :sleeper]) (apply safely/sleeper retry-delay))]
      (-> sm
          (assoc-in [:latest-errors from-state :sleeper] nap)
          (assoc :sleeper {:nap nap :return-state from-state})
          (assoc :state :machina/sleep))

      )
    ;; TODO: exception or halted state?
    (throw (ex-info "no default error policy" sm)))
  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment



  (comment
    (sleep-state
     (error-dispatch
      ((error-state
        (fn [_] (throw (ex-info "boom" {}))))
       {:state :foo
        :error-policies
        {:machina/default {:type        :retry
                           :max-retry   :forever
                           :retry-delay [:random-exp-backoff :base 3000 :+/- 0.35 :max 25000]}}

        :wrappers
        [#'epoch-counter #'log-state-change #'error-state]}))))


  (defn- wrapper
    ([] identity)
    ([f] f)
    ([f g] (g f))
    ([f g & fs]
     (reduce wrapper (concat [f g] fs))))

  (comment
    (defn mh [n]
      (fn [h]
        (fn [sm]
          (println n)
          (h sm))))

    (def f1 (mh 1))
    (def f2 (mh 2))
    (def f3 (mh 3))
    (def f4 (mh 4))

    ((wrapper identity f4 f3 f2 f1) {}))


  ;; write to file example
  (def sm
    {:state :machina/start
     :epoch 0
     :data nil

     :dispatch
     {:machina/stop identity
      :machina/start
      (fn
        [sm]
        (assoc sm
               :data "/tmp/1/2/3/4/5/file.txt"
               :state :write-to-file))

      :write-to-file
      (fn
        [{f :data :as sm}]
        (write-to-file f)
        (assoc sm
               :state :machina/sleep
               :sleeper {:nap 1000 :return-state :write-to-file}))

      :machina/sleep sleep-state
      :machina/error error-dispatch
      }

     :error-policies
     {:machina/default {:type        :retry
                        :max-retry   :forever
                        :retry-delay [:random-exp-backoff :base 300 :+/- 0.35 :max 25000]}

      :_write-to-file   {:type        :retry
                         :max-retry   :forever
                         :retry-delay [:random 3000 :+/- 0.35]}}

     :wrappers
     [#'epoch-counter #'log-state-change #'error-state]})


  (defn bad-state [sm]
    (throw (ex-info "Invalid state" sm)))

  (defn transition
    [{:keys [state data dispatch wrappers] :as sm}]
    (let [f0 (get dispatch state bad-state)
          f  (apply wrapper (cons f0 (reverse wrappers)))]
      (f sm)))


  (comment

    (-> sm
        transition
        transition
        transition
        transition
        transition
        )

    (->> sm
         (iterate transition)
         (take 20)
         #_(take-while #(not= :machina/stop (:state %)))
         (last))


    ))


(comment

  (defn write-to-file [f]
    (spit f
          (str (java.util.Date.) \newline)
          :append true))

  )
