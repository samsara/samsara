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


(defn log-state-change
  "This wrapper logs the state machine transitions to the configured
  logger. The default log level is :debug, but you can specify a
  different level."
  ([handler]
   (log-state-change :debug handler))
  ([level handler]
   (fn [sm1]
     (let [sm2 (handler sm1)]
       (log/logp (or level :debug) "transition:" (:state sm1) "->" (:state sm2))
       sm2))))


(defn epoch-counter
  "This wrapper increments a counter on every transition.
  Useful to determine whether the state machine is progressing."
  [handler]
  (fn [sm]
    (handler (update sm :epoch (fnil inc 0)))))



(comment

  (defn error-state
    [handler]
    (fn [{:keys [state epoch] :as sm}]
      (try
        (-> (handler sm)
            ;; clear error flag
            (update :latest-errors dissoc state));; TODO: dissoc-in
        (catch Throwable x
          (-> sm
              (assoc-in  [:latest-errors :from-state] state)
              (update-in [:latest-errors state :repeated] (fnil inc 0))
              (update-in [:latest-errors state]
                         #(merge % {:error-epoch epoch
                                    :error       x}))
              (assoc :state :machina/error))))))


  (defn error-dispatch
    [{{:keys [from-state]} :latest-errors
      error-policies :error-policies :as sm}]
    (if-let [policy (or (get error-policies from-state) (get error-policies :machina/default))]
      ;; TODO: multimethod dispatch by type?
      (let [{:keys [retry-delay]} policy
            nap (or (get-in sm [:latest-errors from-state :sleeper]) (apply safely/sleeper retry-delay))]
        (-> sm
            (assoc-in [:latest-errors from-state :sleeper] nap)
            (assoc :sleeper {:nap nap :return-state from-state})
            (assoc :state :machina/sleep))

        )
      (throw (ex-info "no default error policy" sm)))
    )


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

  ;;
  ;;{:state   :machina/sleep
  ;; :sleeper {:nap (safely/sleeper :random 3000 :+/- 0.35) ;; or a number of millis 1000
  ;;           :return-state :foo}}
  ;;
  (defn sleep-state [{{:keys [nap return-state]} :sleeper :as sm}]
    (when-not (and (or (fn? nap) (number? nap)) return-state)
      (throw (ex-info "invalid sleep state." sm)))

    ;; nap a little bit
    (if (number? nap)
      (safely/sleep nap)
      (nap))

    (-> sm
        (assoc  :state  return-state)
        (dissoc :sleeper)))


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
