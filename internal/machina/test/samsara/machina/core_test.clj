(ns samsara.machina.core-test
  (:require [samsara.machina.core :refer :all]
            [midje.sweet :refer :all]))


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
 )
