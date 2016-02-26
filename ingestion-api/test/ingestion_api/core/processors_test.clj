(ns ingestion-api.core.processors-test
  (:require [ingestion-api.core.processors :refer :all]
            [clojure.java.io :as io])
  (:require [midje.sweet :refer :all]))


;; tests for the ingestion-api's core processing functions


(facts "events validation:"


       ;; single valid event
       (is-invalid? [{:timestamp 1
                      :eventName "sample.event"
                      :sourceId  "dev1"}])
       => nil


       ;; empty set
       (is-invalid? [])
       => nil


       ;; single event is not valid
       ;; as a collection of events is expected
       ;; as payload
       (is-invalid? {:timestamp 1
                     :eventName "sample.event"
                     :sourceId  "dev1"})
       =not=> nil


       ;; making sure that it doesn't throw exceptions
       ;; if payload isn't what it expects
       (is-invalid? "a string")                 =not=> nil
       (is-invalid? (bytes [1 2 3]))            =not=> nil
       (is-invalid? (io/input-stream "stream")) =not=> nil


       ;; multiple valid events
       (is-invalid? [{:timestamp 1
                      :eventName "sample.event"
                      :sourceId  "dev1"}
                     {:timestamp 2
                      :eventName "sample.event"
                      :sourceId  "dev1"}
                     {:timestamp 3
                      :eventName "sample.event"
                      :sourceId  "dev1"}])
       => nil


       ;; mixed valid/invalid events
       (let [errors
             (is-invalid? [{:timestamp 1
                            :eventName "sample.event"
                            :sourceId  "dev1"}
                           {:timestamp 2
                            :missing "the eventName field"
                            :sourceId  "dev1"}
                           {:timestamp 3
                            :eventName "sample.event"
                            :sourceId  "dev1"}])]


         ;; overall errors
         errors         =not=> nil

         ;; individual fields
         (first errors)  => nil
         (second errors) =not=> nil
         (last  errors)  => nil
         )
       )



(facts "inject-receivedAt should inject the system timestamp unless it is already present (never overwrite)"


       ;; testing successful injection
       (inject-receivedAt 10 [{:timestamp 1
                               :eventName "sample.event"
                               :sourceId  "dev1"}
                              {:timestamp 2
                               :missing "the eventName field"
                               :sourceId  "dev1"}
                              {:timestamp 3
                               :eventName "sample.event"
                               :sourceId  "dev1"}])
       => [{:timestamp 1
            :eventName "sample.event"
            :sourceId  "dev1"
            :receivedAt 10}
           {:timestamp 2
            :missing "the eventName field"
            :sourceId  "dev1"
            :receivedAt 10}
           {:timestamp 3
            :eventName "sample.event"
            :sourceId  "dev1"
            :receivedAt 10}]


       ;; testing avoid overwrite
       (inject-receivedAt 10 [{:timestamp 1
                               :eventName "sample.event"
                               :sourceId  "dev1"
                               :receivedAt 2}])
       => [{:timestamp 1
            :eventName "sample.event"
            :sourceId  "dev1"
            :receivedAt 2}]

       )
