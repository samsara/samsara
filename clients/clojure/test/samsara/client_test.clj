(ns samsara.client-test
  (:require [samsara.client :refer :all]
            [samsara.ring-buffer :refer :all]
            [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [samsara.utils :refer [to-json gunzip-string]]))

(testable-privates samsara.client record-event-in-buffer)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                ---==| V A L I D A T E - E V E N T S |==----                ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(fact "validate-events: validates a single event"

      ;; valid
      (validate-events :single
                       {:eventName "a" :timestamp 1 :sourceId "d1"}) => nil
      (validate-events :single
                       {:eventName "a" :timestamp 1 :sourceId "d1"
                        :foo 1 :bar "baz" :bax 123456789.102}) => nil

      ;; invalid
      (validate-events :single
                       {}) =not=> nil


      ;; invalid
      (validate-events :single
                       {:eventName2 "a" :timestamp 1 :sourceId "d1"}) =not=> nil
      (validate-events :single
                       {:eventName "a" :timestamp2 1 :sourceId "d1"}) =not=> nil
      (validate-events :single
                       {:eventName "a" :timestamp 1 :sourceId2 "d1"}) =not=> nil
      )



(fact "validate-events: validates a bunch events"

      ;; valid
      (validate-events :batch
                       []) => nil
      (validate-events :batch
                       [{:eventName "a" :timestamp 1 :sourceId "d1"}]) => nil
      (validate-events :batch
                       [{:eventName "a" :timestamp 1 :sourceId "d1"}
                        {:eventName "a" :timestamp 1 :sourceId "d1"
                         :foo 1 :bar "baz" :bax 123456789.102}]) => nil

      ;; invalid
      (validate-events :batch
                       {:eventName2 "a" :timestamp 1 :sourceId "d1"}) =not=> nil
      (validate-events :batch
                       [;;valid
                        {:eventName "a" :timestamp 1 :sourceId "d1"}
                        ;;invalid
                        {:eventName "a" :timestamp2 1 :sourceId "d1"}]) =not=> nil
      )



(fact "validate-events: doesn't accept any other case"

      (validate-events :something-else
                       [{:eventName "a" :timestamp 1 :sourceId "d1"}]
                       ) => (throws Exception)

      )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| P U B L I S H - E V E N T S |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro with-mock-send-events
  "creates a mock version of send-events which returns an array
   containing the parameters which were passed to the function
   itself. The parameters are then made available as local
   binding for ease of test. These parameters are named:
   `url`, `headers`, `body` and `opts`."
  [publish & body]
  `(with-redefs [samsara.client/send-events (fn [& args#] args#)]
     (let [[~'url ~'headers ~'body ~'opts] ~publish]
       ~@body)))


(fact "publish-events: should post the given events to the ingestion-api
       (no compression)"

      ;; successful post of a single event in a batch
      (with-mock-send-events
        (publish-events "http://localhost:9000"
                        [{:eventName "a" :timestamp 1 :sourceId "d1"}]
                        {:compression :none})

        url => "http://localhost:9000/v1/events"

        headers => (contains {"Content-Type" "application/json"
                              PUBLISHED-TIMESTAMP anything})

        body => (to-json [{:eventName "a" :timestamp 1 :sourceId "d1"}] )

        )
      )



(fact "publish-events: should post the given events to the ingestion-api
       by default should compress the events"

      ;; successful post of a single event in a batch
      (with-mock-send-events
        (publish-events "http://localhost:9000"
                        [{:eventName "a" :timestamp 1 :sourceId "d1"}])

        url => "http://localhost:9000/v1/events"

        headers => (contains {"Content-Type" "application/json"
                              PUBLISHED-TIMESTAMP anything})

        (gunzip-string body) =>  (to-json [{:eventName "a" :timestamp 1 :sourceId "d1"}] )

        )
      )




(fact "publish-events: compression gzip compression should add appropriate headers"

      ;; successful post of a single event in a batch
      (with-mock-send-events
        (publish-events "http://localhost:9000"
                        [{:eventName "a" :timestamp 1 :sourceId "d1"}]
                        {:compression :gzip})

        url => "http://localhost:9000/v1/events"

        headers => (contains {"Content-Type" "application/json"
                              "Content-Encoding" "gzip"
                              PUBLISHED-TIMESTAMP anything})

        opts => (contains {:compression :gzip})

        (gunzip-string body) =>  (to-json [{:eventName "a" :timestamp 1 :sourceId "d1"}] )

        )
      )


(fact "publish-events: invalid events cause exception"

      ;; invalid event
      (with-mock-send-events
        (publish-events "http://localhost:9000"
                        [{:invalid "event"}])
        ) => (throws Exception)


      ;; invalid single events not allowed
      (with-mock-send-events
        (publish-events "http://localhost:9000"
                        {:eventName "a" :timestamp 1 :sourceId "d1"})
        ) => (throws Exception)
      )



(fact "record-event-in-buffer: should validate the event and only when valid
       add it to the buffer"

      ;; valid event
      (items
       (record-event-in-buffer (ring-buffer 3) {}
                               {:eventName "a" :timestamp 1 :sourceId "d1"}))

      => [{:eventName "a" :timestamp 1 :sourceId "d1"}]


      ;; invalid event
      (items
       (record-event-in-buffer (ring-buffer 3) {}
                               {:timestamp 1 :sourceId "d1"}))

      => (throws Exception)

      )





(fact "record-event-in-buffer: should add the configured sourceId when not present
      in the event"

      ;; sourceId is present in the event so no override
      (items
       (record-event-in-buffer (ring-buffer 3)
                               {:sourceId "d0"}
                               {:eventName "a" :timestamp 1 :sourceId "d1"}))

      => [{:eventName "a" :timestamp 1 :sourceId "d1"}]


      ;; sourceId is present in the event so no override, not present in cfg
      (items
       (record-event-in-buffer (ring-buffer 3)
                               {}
                               {:eventName "a" :timestamp 1 :sourceId "d1"}))

      => [{:eventName "a" :timestamp 1 :sourceId "d1"}]


      ;; sourceId is NOT present in the event so use the config one
      (items
       (record-event-in-buffer (ring-buffer 3)
                               {:sourceId "d0"}
                               {:eventName "a" :timestamp 1}))

      => [{:eventName "a" :timestamp 1 :sourceId "d0"}]




      ;; sourceId is NOT present in the event and NOT present
      ;; in the config so exception
      (items
       (record-event-in-buffer (ring-buffer 3)
                               {}
                               {:eventName "a" :timestamp 1}))

      => (throws Exception)

      )
