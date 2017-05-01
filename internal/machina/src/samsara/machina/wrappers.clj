(ns samsara.machina.wrappers
  (:refer-clojure :exclude [error-handler])
  (:require [clojure.tools.logging :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;            ---==| M I D D L E W A R E   W R A P P E R S |==----            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn log-state-change
  "This wrapper logs the state machine transitions to the configured
  logger. The default log level is :debug, but you can specify a
  different level."
  ([handler]
   (log-state-change :debug handler))
  ([level handler]
   (fn [sm1]
     (let [sm2 (handler sm1)]
       (log/logp (or level :debug) "transition:" (:state sm1) "->" (:state sm2))
       sm2))))



(defn epoch-counter
  "This wrapper increments a counter on every transition.
  Useful to determine whether the state machine is progressing."
  [handler]
  (fn [sm]
    (handler (update sm :machina/epoch (fnil inc 0)))))



(defn error-handler
  "This wrapper traps exceptions from the underlying handler
   and setup the error information under the `:machina/latest-errors`
   key and make a transition to the `:machina/error` state"
  [handler]
  (fn [{:keys [state machina/epoch] :as sm}]
    (try
      (-> (handler sm)
          ;; clear error flag in case of successful transition
          (update :machina/latest-errors dissoc state));; TODO: dissoc-in
      (catch Throwable x
        (-> sm
            (assoc-in  [:machina/latest-errors :machina/from-state] state)
            (update-in [:machina/latest-errors state :repeated] (fnil inc 0))
            (update-in [:machina/latest-errors state]
                       #(merge % {:error-epoch epoch
                                  :error       x}))
            (assoc :state :machina/error))))))
