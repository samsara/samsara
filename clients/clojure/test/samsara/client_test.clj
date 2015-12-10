(ns samsara.client-test
  (:require [samsara.client :refer :all]
            [midje.sweet :refer :all]
            [samsara.utils :refer [to-json]]))


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


(defmacro with-mock-send-events [publish & body]
  `(with-redefs [samsara.client/send-events (fn [& args#] args#)]
     (let [[~'url ~'headers ~'body ~'opts] ~publish]
       ~@body)))


(fact "publish-events: should post the given events to the ingestion-api"

      ;; successful post of a single event in a batch
      (with-mock-send-events
        (publish-events "http://localhost:9000/v1"
                        [{:eventName "a" :timestamp 1 :sourceId "d1"}])

        url => "http://localhost:9000/v1/events"

        headers => (contains {"Content-Type" "application/json"
                              "X-Samsara-publishedTimestamp" anything})

        body => (to-json [{:eventName "a" :timestamp 1 :sourceId "d1"}] )

        )
      )



(fact "publish-events: invalid events cause exception"

      ;; invalid event
      (with-mock-send-events
        (publish-events "http://localhost:9000/v1"
                        [{:invalid "event"}])
        ) => (throws Exception)


      ;; invalid single events not allowed
      (with-mock-send-events
        (publish-events "http://localhost:9000/v1"
                        {:eventName "a" :timestamp 1 :sourceId "d1"})
        ) => (throws Exception)
      )
