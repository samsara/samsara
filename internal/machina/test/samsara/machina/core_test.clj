(ns samsara.machina.core-test
  (:require [samsara.machina.core :refer :all]
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
 "sleep-transition"

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
 "wrapper-error-handler"

 (fact "it traps the uncaught errors and set the machine into a :machina/error state"

       (let [sm ((wrapper-error-handler (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
                 {:state :foo})]

         ;; set the state to :machina/error
         (:state sm) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm :machina/latest-errors :from-state) => :foo

         ;; add the error event in the :machina/latest-errors
         (-> sm :machina/latest-errors :foo :error ex-data) => {:cause :boom}))



 (fact "the `:repeated` counter is increased for consecutive errors"

       ;;simulate two consecutive errors on the same state
       (let [fs (wrapper-error-handler (fn [sm] (throw (ex-info "boom" {:cause :boom}))))
             sm (fs (assoc (fs {:state :foo}) :state :foo))]

         ;; set the state to :machina/error
         (:state sm) => :machina/error

         ;; set the provenance in the :machina/latest-errors
         (-> sm :machina/latest-errors :from-state) => :foo

         ;; the counter has been increased
         (-> sm :machina/latest-errors :foo :repeated) => 2))



 (fact "a successful state transition clear the :machina/latest-errors
        for that state and reset the `:repeated` counter"

       ;;simulate a successful run after an error
       (let [fs (wrapper-error-handler
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
         (-> sm1 :machina/latest-errors :from-state) => :foo

         ;; the counter has been increased
         (-> sm1 :machina/latest-errors :foo :repeated) => 1))

 )
