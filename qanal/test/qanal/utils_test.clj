(ns qanal.utils-test
  (:use midje.sweet)
  (:require [qanal.utils :refer :all]))

(def ^:private called? (atom nil))
(defn- callme [& args]
  (if (nil? args)
    (reset! called? true)
    (reset! called? args)))

(facts "About execute-if-elapsed"
       (with-state-changes [(before :facts (reset! called? nil))]
                           (fact "execute if :last-time-executed isn't present"
                                 (execute-if-elapsed {} 5000 callme) @called? => truthy)
                           (fact "return :last-time-executed when not present"
                                 (execute-if-elapsed {} 5000 callme) => (contains {:last-time-executed number?}))
                           (fact "execute if elapsed is greater than window"
                                 (execute-if-elapsed {:last-time-executed (- (System/currentTimeMillis) 7000)} 5000 callme)
                                 @called?
                                 => truthy)
                           (fact "don't execute if elapsed is less than window"
                                 (execute-if-elapsed {:last-time-executed (- (System/currentTimeMillis) 3000)} 5000 callme)
                                 @called?
                                 => falsey)))


(facts "About result-or-exception"
       (fact "Catch and return the exception"
             (result-or-exception (fn [] (/ 1 0))) => #(instance? Exception %))
       (fact "Execute and return the result"
             (result-or-exception (fn [] (/ 0 1))) => #(= 0 %)))



