(ns samsara.machina.core
  (:require [clojure.tools.logging :as log]
            [safely.core :as safely]))

;;
;; This namespace contains an REPL exploration session on
;; various possible implementation of a State Machine
;; in Clojure. There are several characteristics that
;; I'm keen to explore and achieve in the final system
;; such as: simplicity, flexibility, robustness,
;; speed, and more.
;; I'll be writing several implementations of the
;; state machine to solve a simple problem
;; and try to evaluate which compromises I'm making
;; in regards to the aforementioned characteristics.
;;


;;
;; TODO: managed transition
;; TODO: error management
;; TODO: auto-retry with exponential backoff
;; TODO: start and stop states
;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;            ---==| M I D D L E W A R E   W R A P P E R S |==----            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn wrapper-log-state-change
  "This wrapper logs the state machine transitions to the configured
  logger. The default log level is :debug, but you can specify a
  different level."
  ([handler]
   (wrapper-log-state-change :debug handler))
  ([level handler]
   (fn [sm1]
     (let [sm2 (handler sm1)]
       (log/logp (or level :debug) "transition:" (:state sm1) "->" (:state sm2))
       sm2))))



(defn wrapper-epoch-counter
  "This wrapper increments a counter on every transition.
  Useful to determine whether the state machine is progressing."
  [handler]
  (fn [sm]
    (handler (update sm :epoch (fnil inc 0)))))



(defn wrapper-error-handler
  "This wrapper traps exceptions from the underlying handler
   and setup the error information under the `:machina/latest-errors`
   key and make a transition to the `:machina/error` state"
  [handler]
  (fn [{:keys [state epoch] :as sm}]
    (try
      (-> (handler sm)
          ;; clear error flag in case of successful transition
          (update :machina/latest-errors dissoc state));; TODO: dissoc-in
      (catch Throwable x
        (-> sm
            (assoc-in  [:machina/latest-errors :from-state] state)
            (update-in [:machina/latest-errors state :repeated] (fnil inc 0))
            (update-in [:machina/latest-errors state]
                       #(merge % {:error-epoch epoch
                                  :error       x}))
            (assoc :state :machina/error))))))



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

    ;; nap a little bit
    (do
      (cond
        (fn?     nap) (nap)
        (vector? nap) (apply safely/sleeper nap)
        (number? nap) (safely/sleep nap))

      (-> sm
          (assoc  :state  return-state)
          (dissoc :machina/sleep)))))



(defn error-transition
  "Make the transition from `:machina/error` to the managed target state
  it uses the defined `:machina/error-policies` and the data
  from `:machina/latest-errors`
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
