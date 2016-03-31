(ns samsara.qanal.state-machine
  (:require [safely.core :refer [sleeper]]))

;;
;; This is the implementation of the state machine described
;; at doc/state-machine.md
;;

;;
;; Available states
;;
;; :extract/init
;; :extract/check
;; :extract/poll
;; :retry
;; :transform/parse
;; :transform/validate
;; :transform/transform
;; :load/init
;; :load/bulk-load
;;

;; sample state
{:config {:topic "topic1" :partition 2}
 :fns {:init-consumer (fn [cfg] :a-consemer)}

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


(defn retry-error [s e]
  (-> s
      (update-in [:retry :state] (fn [os] (if (not= :retry (:state s)) (:state s) os)))
      (update-in [:retry :attempts] (fn [ov] (inc (or ov 1))))
      (update-in [:retry :delayer] (fn [ov] (or ov (sleeper :random-exp-backoff :base 300 :+/- 50/100 :max 90000))))
      (assoc-in  [:retry :last-error] e)
      (assoc      :state :retry)))


(defmethod transition :retry
  [s]
  (with-state-machine s
    :on-error   retry-error
    :on-success (fn [{{:keys [state]} :retry :as s}] (assoc s :state state))
    (fn [{{:keys [delayer]} :retry}]
      (delayer)
      nil)))


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


;; TODO: err: 4,5

(defmethod transition :extract
  [s]
  (with-state-machine s
    :on-error   retry-error
    :on-success (fn [s]
                  (if (empty? (get-in s [:data :batch]))
                    s
                    (clean-move-to :transform s)))

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


(defmethod transition :transform
  [s]
  (with-state-machine s
    :on-error   retry-error
    :on-success (fn [s]
                  (if (empty? (get-in s [:data :batch]))
                    s
                    (clean-move-to :transform s)))

    (fn [{{:keys [parse validate transform]} :fns
         {:keys [batch]} :data
         config :config :as s}]
      (-> s
          extract-init-consumer
          extract-check
          extract-poll))))
