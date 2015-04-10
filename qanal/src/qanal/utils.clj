(ns qanal.utils
  (:require [cheshire.core :as json])
  (:require [taoensso.timbre :as log]))


(defn exit [exit-code msg]
  (println msg)
  (System/exit exit-code))


(defn sleep [millis]
  (try
    (Thread/sleep millis)
    (catch InterruptedException ie
      (log/warn ie "Sleeping Thread was interrupted "))))

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
      e)))

;; Maybe this should be written as a macro  o_O ??
(defn continously-try [f args-list retry error-msg]
  (let [result (apply result-or-exception f args-list)]
    (if-not (instance? Exception result)
      result
      (do
        (log/warn error-msg)
        (log/warn result "Exception during continously-try."
                  "Will retry in " retry " milliseconds")
        (sleep retry)
        (recur f args-list retry error-msg)))))


(defmacro safe-execution [default error-msg print-stack & body]
  `(try
     ~@body
     (catch Exception x#
       (let [value# ~default
             msg#   ~error-msg]
         (if ~print-stack
           (log/warn x# msg#)
           (log/warn msg# x#))
         value#))))


(defmacro safe [default error-msg & body]
  `(safe-execution ~default ~error-msg true
                   ~@body))



(defmacro safe-short [default error-msg & body]
  `(safe-execution ~default ~error-msg false
                   ~@body))


(defn from-json [^String s]
  (when s
    (json/decode s true)))



(defn bytes->string [^bytes b]
  (when b
    (safe nil "Error converting bytes to UTF-8 String."
          (String. b "utf-8"))))
