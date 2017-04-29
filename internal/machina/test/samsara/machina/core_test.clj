(ns samsara.machina.core-test
  (:require [samsara.machina.core :refer :all]
            [midje.sweet :refer :all]))



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
