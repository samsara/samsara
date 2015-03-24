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

(defn execute-if-elapsed

  ([f last-time-executed]
    (execute-if-elapsed f last-time-executed 1000))

  ([f last-time-executed window]
    (let [elapsed (- (System/currentTimeMillis) last-time-executed)]
      #_(log/debug "last-time-executed->" last-time-executed " elapsed->" elapsed)
      (when (> elapsed window)
        {:executed true :result (f)}))))

(defn result-or-exception [f & args-list]
  (try
    (apply f args-list)
    (catch Exception e
      (log/warn e)
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
