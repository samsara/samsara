(ns moebius.kv
  (:refer-clojure :exclude [set get remove]))

(defprotocol KV
  "A key value store protocol"

  (set [kvstore sourceId key value]
    "Set the given key to the give value. Returns the new KV store")

  (get [kvstore sourceId key]
    "It returns the current value of the given key. nil if not found.")


  (snapshot [kvstore]
    "Return the current value of the kvstore")

  (tx-log [kvstore]
    "Return the current Transaction log"))



(defrecord InMemoryKVstore [data]

  KV
  (set [kvstore sourceId key value]
    (InMemoryKVstore.
     (-> (:data kvstore)
         (update-in [:version] inc)
         (assoc-in [:snapshot sourceId key] value)
         ((fn [{:keys [version] :as data}]
            (update-in data [:tx-log] conj [version [sourceId key] value]))))))

  (snapshot [kvstore]
    (-> kvstore :data :snapshot))

  (tx-log [kvstore]
    (-> kvstore :data :tx-log))

  (get [kvstore sourceId key]
    (get-in kvstore [:data :snapshot sourceId key]))
)


(defn make-in-memory-kvstore []
  (InMemoryKVstore. {:version 0 :snapshot {} :tx-log []}))


(comment
 (def kv (make-in-memory-kvstore))

 (def kv1 (set (set kv "a" :test 1) "a" :test 2))

 (set kv "a" :test3 4)

 (kv-get kv "a" :test)
  kv

 (snapshot kv)

 (tx-log kv))
