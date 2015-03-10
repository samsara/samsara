(ns qanal.utils
  (:require [taoensso.timbre :as log]))


(defn exit [exit-code msg]
  (println msg)
  (System/exit exit-code))


(defn sleep [millis]
  (try
    (Thread/sleep millis)
    (catch InterruptedException ie
      (log/warn "Sleeping Thread was interrupted : " ie))))

(defn execute-if-elapsed [{:keys [last-time-executed] :as m} window f & args]
  (let [last-time (or last-time-executed 0)
        elapsed (- (System/currentTimeMillis) last-time)]
    #_(log/debug "last-time->" last-time " elapsed->" elapsed)
    (if (> elapsed window)
      (do
        (apply f args)
        (assoc m :last-time-executed (System/currentTimeMillis)))
      m)))

(defn result-or-exception [f & args-list]
  (try
    (apply f args-list)
    (catch Exception e
      e)))

;; Maybe this should be written as a macro  o_O ??
(defn continously-try [f args-list retry error-msg]
  (let [result (apply result-or-exception f args-list)]
    (if-not (instance? Exception result)
      result
      (do
        (log/warn error-msg)
        (log/warn "Exception : " result)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur f args-list retry error-msg)))))
