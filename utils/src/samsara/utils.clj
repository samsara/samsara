(ns samsara.utils
  (:require [cheshire.core :as json])
  (:require [taoensso.timbre :as log])
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.util.zip GZIPInputStream GZIPOutputStream]
           [java.nio ByteBuffer]))


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


(defn stoppable-thread
  "Execute the function `f` in a separate thread called `name`,
   and it return a function without arguments which when called it
  stop the execution of the thread.  The function `f` can be a
  thunk (function with no arguments) or can optionally have a state
  which is passed on every execution and the result of a prior
  execution of `f` is passed to the next execution. The first time it
  is called with `nil`. When the function `f' expects a sate then
  the option `:with-state true' must be passed.

  Between two execution the thread will sleep of 3000 millis (configurable
  with :sleep-time 5000)

  Ex:

      (def t (stoppable-thread \"hello\" (fn [] (println \"hello world\"))))

      ;; in background you should see every 3s appear the following message
      ;; hello world
      ;; hello world
      ;; hello world

      ;; to stop the thread
      (t)


      (def t (stoppable-thread \"counter\"
               (fn [counter]
                  (let [c (or counter 0)]
                    (println \"counter:\" c)
                    ;; return the next value
                    (inc c)))
               :with-state true
               :sleep-time 1000))

      ;; in background you should see every 1s appear the following message
      ;; counter: 0
      ;; counter: 1
      ;; counter: 2
      ;; counter: 3

      ;; to stop the thread
      (t)

  "
  [name f & {:keys [with-state sleep-time]
             :or {with-state false
                  sleep-time 3000}}]
  (let [stopped (atom false)
        thread
        (Thread.
         (fn []
           (log/debug "Starting thread:" name)
           (loop [state nil]

             (let [new-state
                   (safely name
                    (if with-state (f state) (f)))]

               (safely "sleeping"
                (Thread/sleep sleep-time))

               ;; if the thread is interrupted then exit
               (when-not @stopped
                 (recur new-state)))))
         name)]
    (.start thread)
    ;; return a function without params which
    ;; when executed stop the thread
    (fn []
      (swap! stopped (constantly true))
      (.interrupt thread)
      (log/debug "Stopping thread:" name))))



(defn ^bytes gzip-string
  "Creates a gzip representation of the given string into a byte array"
  ([^String string]
   (gzip-string string "utf-8"))
  ([^String string encoding]
   (when string
     (with-open [out (ByteArrayOutputStream.)
                 gzip (GZIPOutputStream. out)]
       (do
         (.write gzip (.getBytes string encoding))
         (.finish gzip)
         (.toByteArray out))))))



(defn ^String gunzip-string
  "decodes a gzip string from bytes into its original representation"
  ([^bytes gzipped-string]
   (gunzip-string gzipped-string "utf-8"))
  ([^bytes gzipped-string encoding]
   (when gzipped-string
     (with-open [out (ByteArrayOutputStream.)
                 in  (GZIPInputStream. (ByteArrayInputStream. gzipped-string))]
       (io/copy in out)
       (.toString out encoding)))))
