(ns moebius.kv
  "This namespace provides the protocols for a Key/Value store
  and the operations necessary to persist and restore its state.
  There is also an implementation for of an in-memory store
  which relies on Clojure immutable maps."
  (:refer-clojure :exclude [set get update]))

;;
;; TODO: force keys to string?
;;

;;
;; The core of the state management is done via this KeyValue store.
;; As a normal K/V store you can `set`/`get` or delete (`del`)
;; an arbitrary key. The value will stored and persisted together
;; with the output events in such way that if the process fail
;; before persisting the values, the state will be restored
;; to the value they had before processing the requests.
;; Keeping K/V state aligned with the events to process,
;; guarantee consistent values and high processing speed
;; as the processing is done entirely in memory.
;;
;; The K/V store is distributed across all processing machines.
;; The sharding strategy follow the same strategy of the events
;; processing.
;; Part of the key is the `sourceId` of the event you are processing
;; it is required. It is really important to provide the same `sourceId`
;; of the events in order to guarantee that the state is restored in
;; the same place.
;;
;; In order worlds the Dirstibuted K/V Store will be distributed
;; in the exact same way of the events, guaranteeing that a certain
;; key will always be local to a the task which is processing a certain
;; event with the same `sourceId`.
;;

;;
;; This protocol contains the function to manage the state
;; from a pipeline user perspective. The KeyValue store
;; can be backed by several different implementations
;; depending on the runtime configuration.
;;
(defprotocol KV
  "A key value store protocol"

  (set [kvstore sourceId key value]
    "Set the given key to the give value. Returns the new KV store")

  (get [kvstore sourceId key]
    "It returns the current value of the given key. nil if not found.")

  (del [kvstore sourceId key]
    "It deletes the given key from the kv store. Returns a new KV store."))


;;
;; The Transaction Log protocol is attached to a KeyValue store
;; to produce a event stream which can be restored when necessary
;; Also this protocol can have several different implementation
;; depending on the hosting cloud or the chosen solution.
;;
(defprotocol Tx-Log
 "A transaction log protocol"

  (update [kvstore sourceId f]
    "Update a given keys. Returns the new KV store")

  (snapshot [kvstore]
    "Return the current value of the kvstore")

  (tx-log [kvstore]
    "Return the current Transaction log")

  (restore [kvstore tx-log]
    "restore the state of a KV store from a given txlog")

  (flush-tx-log [kvstore tx-log]
    "Flushes out the given tx-log from the pending transactions."))



(defrecord InMemoryKVstore [data]

  ;; implementing KV protocol
  KV
  (set [kvstore sourceId key value]
    (if value
      (update kvstore sourceId #(assoc % key value))
      (del kvstore sourceId key)))


  (get [kvstore sourceId key]
    (get-in kvstore [:data :snapshot sourceId key]))


  (del [kvstore sourceId key]
    (update kvstore sourceId #(dissoc % key)))


  ;; implementing Tx-Log semantics
  Tx-Log
  (update [kvstore sourceId f]
    (InMemoryKVstore.
     (-> (:data kvstore)
         ;; increment version number
         (update-in [:version sourceId] #(inc (or % 0)))
         ;; update the given key
         (update-in [:snapshot sourceId] f)
         ;; add a tx-log record
         ((fn [{:keys [version tx-log snapshot] :as data}]
            (update-in data [:tx-log] conj
                       [sourceId (version sourceId) (snapshot sourceId)]))))))


  (tx-log [kvstore]
    (-> kvstore :data :tx-log))


  (snapshot [kvstore]
    (-> kvstore :data :snapshot))


  (restore [kvstore tx-log]
    (InMemoryKVstore.
     (reduce (fn [state [sourceId version value]]
          ;; restore state only if the tx-log version is bigger
          (if (< (get-in state [:version sourceId] 0) version)
            (-> state
                (assoc-in [:version  sourceId] version)
                (assoc-in [:snapshot sourceId] value))
            state))
        (:data kvstore)
        tx-log)))


  (flush-tx-log [kvstore tx-log]
    (InMemoryKVstore.
     (-> (:data kvstore)
         (update-in [:tx-log] #(apply vector (drop (count tx-log) %)))))))


(defn make-in-memory-kvstore []
  (InMemoryKVstore. {:version {} :snapshot {} :tx-log []}))
