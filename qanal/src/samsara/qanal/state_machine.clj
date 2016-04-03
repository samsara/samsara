(ns samsara.qanal.state-machine
  (:require [safely.core :refer [sleeper]]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;        ---==| S T A T E   M A C H I N E   U T I L I T I E S |==----        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; This is the implementation of the state machine described
;; at doc/state-machine.md
;;

;; sample state
{
 ;;
 ;; the configuration of what this state machine
 ;; has to consume and where all the data
 ;; has to be loaded it is part of the `:config` element
 ;;
 :config {;; this is the connect string for the kafka
          ;; brokers, a comma-separated list
          ;; of host and ports
          :brokers ""

          ;; this is the topic and partition to consume
          :topic "topic1" :partition 2

          ;; the elasticsearch endpoint where
          ;; to load the data
          :elasticsearch ""
          }

 ;; TODO: signatures and doc
 :fns {:init-consumer (fn [cfg] :new-consumer)
       :check (fn [c cfg] {["topic1" 2] 2345})
       :poll (fn [c offset] [1 2 3 4])
       :parse    1
       :validate 1
       :tranform 1
       :init-load 1
       :bulk-load 1}

 :state :retry

 :retry {:state :extract :attempts 3 :delayer (fn [])}

 :data {:consumer nil :els nil
        :offsets {["topic1" 2] 2345}
        :batch []}
 }


(defmulti transition :state)



#_(defmethod transition :default [{:keys [state] :as s}]
  (ex-info (str "IllegalStateException: Undefined state: " state) s))


(defmacro with-state-machine
  "to simplify correct state transition and operation.
   to use in the following way.

      (with-state-machine s
        :on-error   (fn [s e]) ;; handle error
        :on-success (fn [s])   ;; transition to new state
        (fn [s]
           ;; process current state
           ))
  "
  [s oek oef osk osf pf]
  `(if (or (not= ~oek :on-error) (not= ~osk :on-success))
     (throw (ex-info "Invalid parameters: the :on-error key or the :on-success are missing or misplaced." {}))
     (let [oef# ~oef
           osf# ~osf
           pf#  ~pf
           s#   ~s]
       (try
         (osf# (or (pf# s#) s#))
         (catch Exception x#
           (oef# s# x#))))))


(defn move-to
  ([state s]
   (assoc s :state state)))


(defn clean-move-to
  ([state s]
   (-> s
       (assoc :state state)
       (dissoc :retry))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                ---==| E R R O R S   H A N D L I N G |==----                ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def errors-map
  {:errors/uncategorized           0
   :errors/kafka-unavailable       1
   :errors/topic-not-found         2
   :erorrs/partition-not-found     3
   :errors/message-too-big         4
   :errors/offset-not-available    5
   :errors/network-error           6
   :errors/invalid-message-format  7
   :errors/invalid-event           8
   :errors/transformation-error    9
   :errors/els-unavailable        10
   :errors/els-status-5xx         11
   :errors/bulk-load-with-errrors 12
   :errors/els-status-4xx         13})



(defn make-error
  ([type message data]
   (make-error type message data nil))
  ([type message data cause]
   (let [info (merge
               data
               {:errnum (get errors-map type 0)
                :error  type})]
     (ex-info message info cause))))



(defn retry-action [s e]
  (-> s
      (update-in [:retry :state] (fn [os] (if (not= :retry (:state s)) (:state s) os)))
      (update-in [:retry :attempts] (fn [ov] (inc (or ov 1))))
      (update-in [:retry :delayer] (fn [ov] (or ov (sleeper :random-exp-backoff :base 300 :+/- 50/100 :max 90000))))
      (assoc-in  [:retry :last-error] e)
      (assoc      :state :retry)))



;; TODO: this needs access to single event
;; and might be different for every state
(defn send-to-errors [s e])

(defmethod transition :retry
  [s]
  (with-state-machine s
    :on-error   retry-action
    :on-success (fn [{{:keys [state]} :retry :as s}] (assoc s :state state))
    (fn [{{:keys [delayer]} :retry}]
      (delayer)
      nil)))



(defn dispatch-error
  "takes a list of pairs of predicate functions and error-handling functions.
   When a predicate function returns truthy when tested against the :error
   key the associated error-handling function is used and the error
   is dispatched to that function. if `preds-funs` contains an odd number
   of elements, last one is interpreted as `catch-all` and it is used
   when no other predicate function matches. The error handling function
   must accept 2 parameters `[state error]`.

   Examples:

     (dispatch-error

       #{:errors/network-error
         :errors/kafka-unavailable}   retry-action

       :errors/message-to-big         send-to-errors

       :errors/offset-not-available   reset-offset

       #(= % :errors/topic-not-found) retry-action
       :errors/partition-not-found    retry-action

      (fn [s e]
         (throw (make-error :errors/uncategorized \"Unknown error\" {} e))))

  "
  [& preds-funs]
  (let [has-default? (odd? (count preds-funs))
        default (if has-default? (last preds-funs) (fn [s e] (throw e)))
        pnfs (if has-default? (drop-last preds-funs) preds-funs)
        pairs (partition 2 pnfs)]
    (fn [s e]
      (let [error (:error (ex-data e))]
        (loop [[[p f] & preds] pairs]
          (cond
            (and (nil? p) (nil? f))        (default s e)
            (and (keyword? p) (= p error)) (f s e)
            (p error)                    (f s e)
            :default                     (recur preds)))))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| S T A T E :   E X T R A C T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

{:config {:topic "topic1" :partition 2}
 :fns {:init-consumer (fn [cfg] :a-consemer)}

 :state :extract

 :retry {:state :extract :attempts 3 :delayer (fn [])}

 :data {:consumer nil :els nil
        :offsets {["topic1" 2] 2345}
        :batch []}
 }



(defn- extract-init-consumer
  [{{:keys [init-consumer]} :fns
    {:keys [consumer]} :data
    config :config :as s}]
  ;; if consumer is not initialized
  ;; the initialize one
  (if consumer
    s
    (assoc-in s [:data :consumer] (init-consumer config))))



(defn- extract-check
  [{{:keys [check]} :fns
    {:keys [consumer offsets]} :data
    config :config :as s}]
  ;; checks if the topics/partitions exists
  ;; and return a map of last committed offsets
  (if offsets
    s
    (assoc-in s [:data :offsets] (check consumer config))))



(defn- extract-poll
  [{{:keys [poll]} :fns
    {:keys [consumer offsets]} :data
    config :config :as s}]
  (assoc-in s [:data :batch] (poll consumer offsets)))


(defmethod transition :extract
  [s]
  (with-state-machine s
    ;; error handling
    :on-error   (dispatch-error
                 :errors/kafka-unavailable    retry-action
                 :errors/topic-not-found      retry-action
                 :errors/partition-not-found  retry-action
                 :errors/network-error        retry-action

                 ;; TODO: err: 4,5
                 :errors/message-too-big      send-to-errors
                 ;;:errors/offset-not-available reset-offset
                 )

    ;; successful dispatch
    :on-success (fn [s]
                  (if (empty? (get-in s [:data :batch]))
                    s
                    (clean-move-to :transform s)))

    ;; this state processing.
    (fn [s]
      (-> s
          extract-init-consumer
          extract-check
          extract-poll))))


(comment

  (transition

   {:config {:topic "topic1" :partition 2}
    :fns {:init-consumer (fn [cfg] :a-consemer)
          :check (fn [c cfg] {["topic1" 2] 2345})
          :poll (fn [c offset] [])}
    :state :extract
    :data {:consumer nil :els nil
           :offsets {1 2}
           }
    })
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;               ---==| S T A T E :   T R A N S F O R M |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: batch -> {:success [] :failed [] :offsets {["t1" 2] 3445}
;; TODO: parse -> validate -> transform take above map
;; and do transformation.
;; end result is: `:success` contains all transformed items
;; and `:failed` contains all failed items with a structure
;; as follow {:error the-execption :topic t1 :partition 1 :offset 1 }
;; and the exception should contain :data with the info of why this
;; particular item failed

(defmethod transition :transform
  [s]
  (with-state-machine s
    ;; error handling
    :on-error   (dispatch-error
                 :errors/invalid-message-format send-to-errors
                 :errors/invalid-event          send-to-errors
                 :errors/transformation-error   send-to-errors
                 )

    ;; successful dispatch
    :on-success (clean-move-to :load s)

    ;; this state processing.
    (fn [{{:keys [parse validate transform]} :fns
         config :config :as s}]
      (update-in s [:data :batch]
                 (fn [batch]
                   (-> {:success batch :failed [] :offsets {}} ;;TODO: calc offsets
                       parse
                       validate
                       transform))))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ---==| S T A T E :   L O A D |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod transition :load
  [s]
  (with-state-machine s
    ;; error handling
    :on-error   (dispatch-error
                 :errors/network-error        retry-action
                 :errors/els-unavailable      retry-action
                 :errors/els-status-5xx       retry-action
                 :errors/els-status-4xx       retry-action

                 :errors/bulk-load-with-errrors send-to-errors
                 )

    ;; successful dispatch
    :on-success (clean-move-to :extract s)

    ;; this state processing.
    (fn [s]
      (-> s
          #_load-init
          #_load-bulk-load))))
