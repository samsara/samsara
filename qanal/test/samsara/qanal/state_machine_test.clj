(ns samsara.qanal.state-machine-test
  (:require [samsara.qanal.state-machine :refer :all])
  (:require [midje.sweet :refer :all])
  (:require [safely.core :refer [*sleepless-mode*]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ---==| S T A T E :   R E T R Y |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(fact "RETRY: an error during state processing causes the transition to :retry state"

      ;; an error causes in a retry-able state casues the state machine
      ;; to transition to a retry state
      (transition
       {:fns {:init-consumer (fn [cfg] (throw (make-error :errors/network-error "net-err" {})))}
        :state :extract
        })
      =>
      (contains
       {:state :retry
        :retry (contains {:state :extract :attempts 2})})

      )




(fact "RETRY: consecutive errors increase the attempts"

      ;; if connection to kafka consumer is not present
      ;; it creates one.
      (binding [*sleepless-mode* true]
        (-> {:fns {:init-consumer (fn [cfg] (throw (make-error :errors/network-error "net-err" {})))}
             :state :extract}
            transition
            transition
            transition))
      =>
      (contains
       {:state :retry
        :retry (contains {:state :extract :attempts 3})})

      )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| S T A T E :   E X T R A C T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(facts ":EXTRACT: entering the state causes the initialization of the kafka consumer if not present yet."

       ;; if connection to kafka consumer is not present it creates one.
       (transition
        {:fns {:init-consumer (fn [cfg] :new-consumer)
               :check (fn [c cfg] {["topic1" 2] 2345})
               :poll (fn [c offset] [1 2 3 4])}
         :state :extract
         })
       =>
       (contains
        {:data (contains {:consumer :new-consumer})
         :state :transform})


      ;; if connection to kafka consumer is already present
      ;; it preserves it
       (transition
        {:fns {:init-consumer (fn [cfg] :new-consumer)
               :check (fn [c cfg] {["topic1" 2] 2345})
               :poll (fn [c offset] [1 2 3 4])}
         :state :extract
         :data {:consumer :old-consumer}
         })
       =>
       (contains
        {:data (contains {:consumer :old-consumer})
         :state :transform})

      )



(facts ":EXTRACT: check if topics/partitions are available and reads last committed offsets"

       ;; if topic doesn't exist the retry later
       (tabular
        (transition
         {:fns {:init-consumer (fn [cfg] :new-consumer)
                :check (fn [consumer cfg] (throw ?ERROR))
                :poll (fn [c offset] [1 2 3 4])}
          :state :extract
          })
        =>
        (contains {:state :retry})

        ?ERROR
        (make-error :errors/kafka-unavailable "" {})
        (make-error :errors/topic-not-found "" {})
        (make-error :errors/partition-not-found "" {})
        (make-error :errors/network-error "" {})

        )


       ;; if topics offsets are already available it preserves them
       (transition
        {:fns {:init-consumer (fn [cfg] :new-consumer)
               :check (fn [c cfg] {["topic1" 2] 2345})
               :poll (fn [c offset] [1 2 3 4])}
         :state :extract
         :data {:consumer :old-consumer
                :offsets {["topic1" 2] 9999}}
         }) =>
       (contains
        {:data (contains {:offsets {["topic1" 2] 9999}})
         :state :transform})

       )


(facts ":EXTRACT: a poll with no new messages ends up in the same state"

       ;; if connection to kafka consumer is not present it creates one.
       (transition
        {:fns {:init-consumer (fn [cfg] :new-consumer)
               :check (fn [c cfg] {["topic1" 2] 2345})
               :poll (fn [c offset] [])}
         :state :extract
         })
       =>
       (contains
        {:state :extract})


       )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| S T A T E :   T R A N S F O R M |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
