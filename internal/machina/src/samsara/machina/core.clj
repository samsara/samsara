(ns samsara.machina.core)

;;
;; TODO: managed transition
;; TODO: error management
;; TODO: auto-retry with exponential backoff
;; TODO: start and stop states
;;



(defn write-to-file [f]
  (spit f
        (str (java.util.Date.) \newline)
        :append true))


(comment
  (defn empty-machine
    "defines a empty state machine which only goes from start->stop"
    []
    {:state :machina/start

     :transitions {:machina/start [:machina/no-op :machina/stop]}})



  (defn configure-machine [sm fns]
    (-> sm
        ;; turn the list of states and transitions into a map
        (update :transitions (fn [ts]
                               (->> (map (fn [[s0 t s1]] [s0 [t s1]]) ts)
                                    (into {})))))))


;;(configure-machine (empty-machine))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ---==| F I R S T   A T T E M P T |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (def sm
    {:state       :machina/start
     :data        nil
     :transitions
     {:machina/start {:fn*        (fnil inc 0)
                      :on-success :machina/start
                      :on-failure :machina/stop}}}
    )

  ;;
  ;; TODO: interesting approach but there is no way
  ;; for the `fn*` to know when to stop
  ;; or to `on-success` to decide which new
  ;; state to go. For example if the counter
  ;; gets to 10 I would like to exit
  ;; Same for the `on-failure` it should be
  ;; able to decide which state based on error
  ;; TODO: `on-success` and `on-failure` should
  ;; take the state in input `(on-success sm)`
  ;; or `(on-failure sm ex)`.
  ;;
  (defn play [{:keys [state transitions data] :as sm}]
    (if (= state :machina/stop)
      sm
      (if-let [{:keys [fn* on-success on-failure]} (get transitions state)]
        (try
          (let [data1 (apply fn* [data])]
            (assoc sm :data data1 :state on-success))
          (catch Exception x
            (assoc sm
                   :error {:from-state state
                           :error x}
                   :state on-failure)))
        (assoc sm :error {:from-state state
                          :error :machina/bad-state}))))


  (comment
    (iterate play sm)
    ))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| S E C O N D   A T T E M P T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(comment



  ;; counter example
  (def sm
    {:state :machina/start
     :data   nil
     :transitions
     {:machina/start {:fn*        (fnil inc 0)
                      :on-success (fn [{:keys [data]}]
                                    (if (= data 10)
                                      :machina/stop
                                      :machina/start))
                      :on-failure (constantly :machina/stop)} }})

  (defn do! [f]
    (fn [v]
      (f v)
      v))


  (defn transition [{:keys [state transitions data] :as sm}]
    (if (= state :machina/stop)
      sm
      (if-let [{:keys [fn* on-success on-failure]} (get transitions state)]
        (try
          (as-> sm $
            (assoc $ :data  (apply fn* [data]))
            (assoc $ :state (on-success $)))
          (catch Exception x
            (as-> sm $
              (assoc $ :state (on-failure $))
              (dissoc $ :error))))
        (assoc sm :error {:from-state state
                          :error :machina/bad-state}))))




  (comment
    (transition sm)
    (->> sm
         (iterate transition)
         (take-while #(not= :machina/stop (:state %)))
         #_(map :data)
         last
         )
    )




  ;; write to file example
  (def sm
    {:state :machina/start
     :data   nil
     :transitions
     {:machina/start {:fn*        (constantly "/tmp/1/2/3/4/5/file.txt")
                      :on-success (constantly :write-to-file)
                      :on-failure (constantly :machina/stop)}


      :sleep {:fn*        (do! (fn [_] (Thread/sleep 1000)))
              :on-success (constantly :write-to-file)
              :on-failure (constantly :sleep)}


      :write-to-file {:fn*        (do! write-to-file)

                      :on-success (fn [sm] (println "OK")   (clojure.pprint/pprint sm) :sleep)
                      :on-failure (fn [sm] (println "FAIL") (clojure.pprint/pprint sm) :sleep)}}})


  (comment

    (-> sm
        transition
        transition
        )

    (->> sm
         (iterate transition)
         (take-while #(not= :machine/stop (:state %)))
         (last))



    )


  )




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ---==| T H I R D   A T T E M P T |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  ;; write to file example
  (def sm
    {:state :machina/start
     :data   nil
     :transitions
     {:machina/start {:fn*        (constantly "/tmp/1/2/3/4/5/file.txt")
                      :on-success (constantly :write-to-file)
                      :on-failure (constantly :machina/stop)}


      :sleep {:fn*        (do! (fn [_] (Thread/sleep 1000)))
              :on-success (constantly :write-to-file)
              :on-failure (constantly :sleep)}


      :write-to-file {:fn*        (do! write-to-file)

                      :on-success (fn [sm] (println "OK")   (clojure.pprint/pprint sm) :sleep)
                      :on-failure (fn [sm] (println "FAIL") (clojure.pprint/pprint sm) :sleep)}}})


  ;;
  ;; Most generic approach
  ;; but not much re-use.
  ;; also hard to make cross-cutting concerns
  ;; like a function which displays all
  ;; the state transitions or even
  ;; measure the time spent in each state.
  ;;
  (defmulti transition :state)

  (defmethod transition :machina/stop
    [sm]
    sm)


  (defmethod transition :machina/start
    [sm]
    (assoc sm
           :data "/tmp/1/2/3/4/5/file.txt"
           :state :write-to-file))


  (defmethod transition :write-to-file
    [{f :data :as sm}]
    (try
      (write-to-file f)
      (assoc sm :state :sleep)
      (catch Exception x
        (assoc sm :state :sleep))))


  (defmethod transition :sleep
    [sm]
    (Thread/sleep 1000)
    (assoc sm :state :write-to-file))

  (comment

    (-> sm
        transition
        transition
        transition
        )

    (->> sm
         (iterate transition)
         (take-while #(not= :machine/stop (:state %)))
         (last))



    ))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ---==| F O U R T H   A T T E M P T |==----                 ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; this time i'm going to use the same basic and generic idea of
;; a f(stm) -> stm' but without the multi-method dispatch
;; so that it is possible to add wrappers/filters style functions
;; which handle cross/cutting concerns.
;;
