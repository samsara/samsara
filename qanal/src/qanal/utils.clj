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

(defn result-or-exception [f & args]
  (try
    (apply f args)
    (catch Exception e
      e)))

(defn continously-try [f args retry error-msg]
  (let [result (result-or-exception f args)]
    (if-not (instance? Exception result)
      result
      (do
        (log/warn error-msg)
        (log/warn "Exception : " result)
        (log/warn "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur f args retry error-msg)))))