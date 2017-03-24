(ns samsara.machina.core)

;;
;; TODO: managed transition
;; TODO: error management
;; TODO: auto-retry with exponential backoff
;; TODO: start and stop states
;;



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
                                  (into {}))))))


;;(configure-machine (empty-machine))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                        ---==| C O U N T E R |==----                        ;;
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


(defn play [{:keys [state transitions data] :as sm}]
  (if (= state :machina/stop)
    sm
    (if-let [{:keys [fn* on-success on-failure]} (get transitions state)]
      (try
        (as-> sm $
          (assoc $ :data (apply fn* [data]))
          (assoc $ :state (on-success $)))
        (catch Exception x
          (as-> sm $
            (assoc $ :error {:from-state state :error x})
            (assoc $ :state (on-failure $))
            (dissoc $ :error))))
      (assoc sm :error {:from-state state
                        :error :machina/bad-state}))))


(comment
  (play sm)
  (->> sm
       (iterate play)
       (take-while #(not= :machina/stop (:state %)))
       #_(map :data)
       last
       )
  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;        ---==| W R I T E   T I M E S T A M P   T O   F I L E |==----        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do! [f]
  (fn [v]
    (f v)
    v))

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


    :write-to-file {:fn*        (do! (fn [f]
                                       (println "write?")
                                       (spit f
                                             (str (java.util.Date.) \newline)
                                             :append true)))

                    :on-success (fn [sm] (println "OK")   (clojure.pprint/pprint sm) :sleep)
                    :on-failure (fn [sm] (println "FAIL") (clojure.pprint/pprint sm) :sleep)}}})


(comment

  (-> sm
      play
      play
      )

  (->> sm
       (iterate play)
       (take-while #(not= :machine/stop (:state %)))
       (last))



  )
