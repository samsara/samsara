(ns samsara.utils
  (:require [cheshire.core :as json])
  (:require [taoensso.timbre :as log]))


(defn to-json
  "Convert a Clojure data structure into it's json pretty print equivalent
   or compact version.
   usage:

   (to-json {:a \"value\" :b 123} :pretty true)
   ;=> {
   ;=>   \"a\" : \"value\",
   ;=>   \"b\" : 123
   ;=> }

   (to-json {:a \"value\" :b 123})
   ;=> {\"a\":\"value\",\"b\":123}
   "
  [data & {:keys [pretty] :or {pretty false}}]
  (if-not data
    ""
    (-> data
        (json/generate-string {:pretty pretty :date-format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"})
        ((fn [s] (if pretty (str s \newline) s))))))



(defn from-json
  "Convert a json string into a Clojure data structure
   with keyword as keys"
  [data]
  (if-not data
    nil
    (-> data
        (json/parse-string true))))



(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))



(defmacro safely [message & body]
  `(let [message# ~message]
     (try
       ~@body
       (catch Throwable x#
         (log/warn x# "Exception during" message#)
         nil))))



(defn invariant [invf]
  (fn [x]
    (safely "invariant operation"
            (invf x))
    x))
