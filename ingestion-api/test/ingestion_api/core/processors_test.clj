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



(facts "inject-publishedAt should inject the given timestamp otherwise leave the event(s) untouched"
       ;; testing injection of :publishedAt
       (inject-publishedAt 1 [{:timestamp 1
                               :eventName "sample.event"
                               :sourceId  "dev1"}
                              {:timestamp 2
                               :eventName "sample.event"
                               :sourceId  "dev1"}
                              {:timestamp 3
                               :eventName "sample.event"
                               :sourceId  "dev1"}])
       => [{:timestamp 1
            :publishedAt 1
            :eventName "sample.event"
            :sourceId  "dev1"}
           {:timestamp 2
            :publishedAt 1
            :eventName "sample.event"
            :sourceId  "dev1"}
           {:timestamp 3
            :publishedAt 1
            :eventName "sample.event"
            :sourceId  "dev1"}]

       ;; testing NON-injection of :publishedAt
       (inject-publishedAt nil [{:timestamp 1
                                 :eventName "sample.event"
                                 :sourceId  "dev1"}
                                {:timestamp 2
                                 :eventName "sample.event"
                                 :sourceId  "dev1"}
                                {:timestamp 3
                                 :eventName "sample.event"
                                 :sourceId  "dev1"}])
       => [{:timestamp 1
            :eventName "sample.event"
            :sourceId  "dev1"}
           {:timestamp 2
            :eventName "sample.event"
            :sourceId  "dev1"}
           {:timestamp 3
            :eventName "sample.event"
            :sourceId  "dev1"}]


       ;; testing NOT overwrite of :publishedAt
       (inject-publishedAt 10 [{:timestamp 1
                                :eventName "sample.event"
                                :sourceId  "dev1"
                                :publishedAt 2}])
       => [{:timestamp 1
            :eventName "sample.event"
            :sourceId  "dev1"
            :publishedAt 2}]


       )



(facts "process-events should validate, inject the :receivedAt
       and :publishedAt (conditionally) on to the sequence of events"

       ;; processing valid event without published times
       (process-events
        [{:timestamp (System/currentTimeMillis)
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"}])
       =>
       (just {:status :success
              :processed-events
              (just [(just {:timestamp integer?
                            :sourceId "a-user",
                            :eventName "test-event",
                            :test-prop "test-value",
                            :receivedAt integer?})])})



       ;; processing valid event which hasn't a publishedAt
       ;; passing a publishedTimestamp (injection case)
       (process-events
        [{:timestamp (System/currentTimeMillis)
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"}]
        :publishedTimestamp 5)
       =>
       (just {:status :success
              :processed-events
              (just [(just {:timestamp integer?
                            :sourceId "a-user",
                            :eventName "test-event",
                            :test-prop "test-value",
                            :receivedAt integer?
                            :publishedAt 5})])})



       ;; processing valid event which has publishedAt
       ;; passing a publishedTimestamp (non overwrite case)
       (process-events
        [{:timestamp (System/currentTimeMillis)
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"
          :publishedAt 3}]
        :publishedTimestamp 5)
       =>
       (just {:status :success
              :processed-events
              (just [(just {:timestamp integer?
                            :sourceId "a-user",
                            :eventName "test-event",
                            :test-prop "test-value",
                            :receivedAt integer?
                            :publishedAt 3})])})



       ;; processing valid event which has receivedAt (non overwrite case)
       (process-events
        [{:timestamp (System/currentTimeMillis)
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"
          :publishedAt 3
          :receivedAt 4}]
        :publishedTimestamp 5)
       =>
       (just {:status :success
              :processed-events
              (just [(just {:timestamp integer?
                            :sourceId "a-user",
                            :eventName "test-event",
                            :test-prop "test-value",
                            :receivedAt 4
                            :publishedAt 3})])})



       ;; processing some invalid events and one valid
       (process-events
        [ ;; valid event
         {:timestamp (System/currentTimeMillis)
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"
          :publishedAt 3}
         ;; BAD: timestamp string
         {:timestamp "a string timestamp"
          :sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"}
         ;; BAD: missing timestamp
         {:sourceId "a-user"
          :eventName "test-event"
          :test-prop "test-value"}]
        :publishedTimestamp 9)
       =>
       (just
        {:status :error,
         :error-msgs
         (just [:OK
                (just {:timestamp anything})
                (just {:timestamp anything})])})


       )
