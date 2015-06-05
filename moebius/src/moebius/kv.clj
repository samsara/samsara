(ns moebius.kv
  (:refer-clojure :exclude [set get remove update]))

;;
;; TODO: multi set function
;; TODO: force keys to string?
;; TODO: Do ve need the version? (think repartitioning)
;; TOOD: tx-log + compaction: single key or all keys x sourceId?
;;

(defprotocol KV
  "A key value store protocol"

  (set [kvstore sourceId key value]
    "Set the given key to the give value. Returns the new KV store")

  (get [kvstore sourceId key]
    "It returns the current value of the given key. nil if not found."))



(defprotocol Tx-Log
  "A transaction log protocol"

  (update [kvstore sourceId key value]
    "Set the given key to the give value. Returns the new KV store")

  (snapshot [kvstore]
    "Return the current value of the kvstore")

  (tx-log [kvstore]
    "Return the current Transaction log")

  (restore [kvstore tx-log]
    "restore the state of a KV store from a given txlog"))



(defrecord InMemoryKVstore [data]

  ;; implementing KV protocol
  KV
  (set [kvstore sourceId key value]
    (update kvstore sourceId key value))


  (get [kvstore sourceId key]
    (get-in kvstore [:data :snapshot sourceId key]))


  ;; implementing Tx-Log semantics
  Tx-Log
  (update [kvstore sourceId key value]
    (InMemoryKVstore.
     (-> (:data kvstore)
         ;; increment version number
         (update-in [:version] inc)
         ;; update the given key
         (assoc-in [:snapshot sourceId key] value)
         ;; add a tx-log record
         ((fn [{:keys [version] :as data}]
            (update-in data [:tx-log] conj [version [sourceId key] value]))))))

  (tx-log [kvstore]
    (-> kvstore :data :tx-log))


  (snapshot [kvstore]
    (-> kvstore :data :snapshot))


  (restore [kvstore tx-log]
    (reduce (fn [kv [_ [sourceId key] value]]
         (update kv sourceId key value))
       kvstore tx-log)))


(defn make-in-memory-kvstore []
  (InMemoryKVstore. {:version 0 :snapshot {} :tx-log []}))


(comment
  ;; create empty kvstores
  (def kv1 (make-in-memory-kvstore))
  (def kv2 (make-in-memory-kvstore))

  ;; both same
  (= kv1 kv2)

  ;; make some changes
  (def kv1'
    (-> kv1
        (set "a" :test 1)
        (set "a" :test 2)))

  ;; restore state based on tx-log
  (def kv2' (restore kv2 (tx-log kv1')))

  ;; still the same
  (= kv1' kv2')
)
