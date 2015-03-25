(ns qanal.utils-test
  (:use midje.sweet)
  (:require [qanal.utils :refer :all]))


(facts "About execute-if-elapsed"
       (fact "execute if elapse is greater than the window"
             (execute-if-elapsed (fn [] (* 5 5)) (- (System/currentTimeMillis) 7000) 5000) => {:executed true :result 25})
       (fact "don't execute if elapsed is less than window"
             (execute-if-elapsed (fn [] (* 5 5)) (- (System/currentTimeMillis) 3000) 5000) => nil))

(facts "About result-or-exception"
       (fact "Catch and return the exception"
             (result-or-exception (fn [] (/ 1 0))) => #(instance? Exception %))
       (fact "Execute and return the result"
             (result-or-exception (fn [] (/ 0 1))) => #(= 0 %)))



