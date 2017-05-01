(ns samsara.machina.core-test
  (:refer-clojure :exclude [error-handler])
  (:require [samsara.machina.wrappers :refer :all]
            [samsara.machina.core :refer :all]
            [midje.sweet :refer :all]))



(defn crash-boom-bang!
  "a utility function which calls the first function in fs
  the first time is called, it calls the second function
  the second time is called and so on. It throws an Exception
  if no more functions are available to fs in a given call."
  [& fs]

  (let [xfs (atom fs)]
    (fn [& args]
      (let [f (or (first @xfs) (fn [&x] (throw (ex-info "No more functions available to call" {:cause :no-more-fn}))))
            _ (swap! xfs next)]
        (apply f args)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;         ---==| S L E E P - T R A N S I T I O N   T E S T S |==----         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(facts
 "on sleep-transition"

 (fact "the `:return-state` is set correctly"
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap 3000 :return-state :foo}}))
       => (contains {:state :foo}))


 (fact "check if `:nap` it works with a number"
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap 3000 :return-state :foo}}))
       => (contains {:state :foo}))


 (fact "check if `:nap` it works with a safely retry spec"
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap [:random 3000 :+/- 0.35]
                           :return-state :foo}}))
       => (contains {:state :foo}))


 (fact "check if `:nap` it works with a function"
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap (fn [])
                           :return-state :foo}}))
       => (contains {:state :foo}))


 (fact "check if :machina/sleep is not valid then
        the machine is halted."
       ;; no sleep key
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep}))
       => (contains {:state :machina/halted})

       ;; no return state
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap (fn [])}}))
       => (contains {:state :machina/halted})

       ;; invalid nap
       (binding [safely.core/*sleepless-mode* true]
         (sleep-transition
          {:state :machina/sleep
           :machina/sleep {:nap {:foo 1}
                           :return-state :foo}}))
       => (contains {:state :machina/halted}))

 )




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;         ---==| E R R O R - T R A N S I T I O N   T E S T S |==----         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(facts
 "on error-handler"

 (fact "it traps the uncaught errors and set the machine into a :machina/error state"

       (let [sm ((error-handler (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
                 {:state :foo})]

         ;; set the state to :machina/error
         (:state sm) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm :machina/latest-errors :machina/from-state) => :foo

         ;; add the error event in the :machina/latest-errors
         (-> sm :machina/latest-errors :foo :error ex-data) => {:cause :boom}))



 (fact "the `:repeated` counter is increased for consecutive errors"

       ;;simulate two consecutive errors on the same state
       (let [fs (error-handler (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (fs (assoc (fs {:state :foo}) :state :foo))]

         ;; set the state to :machina/error
         (:state sm) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm :machina/latest-errors :machina/from-state) => :foo

         ;; the counter has been increased
         (-> sm :machina/latest-errors :foo :repeated) => 2))



 (fact "the `:error-epoch` counter is captured when the epoch-counter is used"

       ;;simulate two consecutive errors on the same state
       (let [fs (epoch-counter
                 (error-handler
                  (fn [sm] (throw (ex-info "boom" {:cause :boom})))))
             sm (fs (assoc (fs {:state :foo}) :state :foo))]

         ;; set the state to :machina/error
         (:state sm) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm :machina/latest-errors :machina/from-state) => :foo

         ;; the epoch counter is captured
         (-> sm :machina/latest-errors :foo :error-epoch) => 2))



 (fact "a successful state transition clear the :machina/latest-errors
        for that state and reset the `:repeated` counter"

       ;;simulate a successful run after an error
       (let [fs (error-handler
                 (crash-boom-bang!
                  (fn [sm] (throw (ex-info "boom1" {:cause :boom1})))
                  #(assoc % :state :foo) ;;simulate ok
                  (fn [sm] (throw (ex-info "boom2" {:cause :boom2})))))

             sm (fs (assoc (fs {:state :foo}) :state :foo))
             sm1 (fs sm)]

         ;; the success step
         (:state sm) => :foo
         ;; error cleared
         (-> sm :machina/latest-errors :foo) => nil

         ;; set the state to :machina/error
         (:state sm1) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm1 :machina/latest-errors :machina/from-state) => :foo

         ;; the counter has been increased
         (-> sm1 :machina/latest-errors :foo :repeated) => 1))

 )



(facts
 "on error-transition"

 (fact "if no error policy is found then the machine should halt"

       (let [fs (error-handler
                 (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (error-transition (fs {:state :foo}))]

         (:state sm) => :machina/halted))



 (fact "if an invalid error policy is found then the machine should halt"

       (let [fs (error-handler
                 (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (error-transition (fs {:state :foo
                                       :machina/error-policies
                                       {:foo 12}}))]

         (:state sm) => :machina/halted))



 (fact "if a new state is provided (keyword) the machine should transition to that state"

       (let [fs (error-handler
                 (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (error-transition (fs {:state :foo
                                       :machina/error-policies
                                       {:foo :baz}}))]

         (:state sm) => :baz))



 (fact "if an handler function is provided the machine should transition to that state"

       (let [fs (error-handler
                 (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (error-transition (fs {:state :foo
                                       :machina/error-policies
                                       {:foo #(assoc % :state :baz)}}))]

         (:state sm) => :baz))


 (fact "if an retry policy has been provided then I expect to transition to
        the sleep state."

       (let [fs (error-handler
                 (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (error-transition (fs {:state :foo
                                       :machina/error-policies
                                       {:foo {:type        :retry
                                              :max-retry   :forever
                                              :retry-delay [:random-exp-backoff :base 3000 :+/- 0.35 :max 25000]}}}))]

         (:state sm) => :machina/sleep
         (-> sm :machina/sleep :return-state) => :foo))
 )
