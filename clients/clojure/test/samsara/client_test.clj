(ns samsara.client-test
  (:require [samsara.client2 :refer :all]
            [midje.sweet :refer :all]))


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
