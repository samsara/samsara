(ns moebius.kv-test
  (:require [moebius.kv :as kv]
            [midje.sweet :refer :all])
  (:import [moebius.kv KV InMemoryKVstore]))



(facts "about KV protocol: I should be able to set a property with any value"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/get "s1" "key1"))         =>       "v1"

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" 1)
           (kv/get "s1" "key1"))         =>       1

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" true)
           (kv/get "s1" "key1"))         =>       true

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" :a-value)
           (kv/get "s1" "key1"))         =>       :a-value)



(facts "about KV protocol: properties are independent across sourceId"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/get "s1" "key1"))         =>       "v1"

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/get "s2" "key1"))         =>       "v2")



(facts "about KV protocol: if a key hasn't been set or delete should return `nil`"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/get "s3" "key1"))         =>       nil

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/get "s1" "key5"))         =>       nil

       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" nil)
           (kv/get "s1" "key1"))         =>       nil)



(facts "about KV protocol: if a key has been deleted should return `nil`"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/del "s1" "key1")
           (kv/get "s1" "key1"))         =>       nil

)



(facts "about KV protocol: deleting a key should have the same effect of set it to `nil`"

       ;; assertion                             expected result
       (let [kv1 (-> (kv/make-in-memory-kvstore)
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/set "s1" "key1" nil)
                     (kv/set "s2" "key2" "vC"))

             kv2 (-> (kv/make-in-memory-kvstore)
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))]

         (kv/snapshot kv1) => (kv/snapshot kv2)))



(facts "about KV protocol: if a key has been deleted (or never set), deleting once more should have any effect"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/del "s1" "key1")
           (kv/del "s1" "key1")
           (kv/del "s1" "key1")
           (kv/get "s1" "key1"))         =>       nil


       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s2" "key1" "v2")
           (kv/del "s3" "key1")
           (kv/del "s3" "key1")
           (kv/del "s3" "key1")
           (kv/get "s3" "key1"))         =>       nil
)


(facts "about Tx-Log protocol: updates must be recorded in tx-log"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/tx-log)
           count)

       =>       2



       ;; this test isn't nice as expose inner structure
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/tx-log))

       => [["s1" 1 {"key1" "v1"}]
           ["s1" 2 {"key1" "v2"}]]



       ;; this test isn't nice as expose inner structure
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/set "s1" "key1" nil)
           (kv/tx-log))

       => [["s1" 1 {"key1" "v1"}]
           ["s1" 2 {"key1" "v2"}]
           ["s1" 3 {}]]


       ;; mix instructions restore
       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))

             kv2 (-> (kv/make-in-memory-kvstore)
                     (kv/restore (kv/tx-log kv1)))]

         (kv/snapshot kv1) => (kv/snapshot kv2))


       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/set "s1" "key1" nil)
                     (kv/set "s2" "key2" "vC"))

             kv2 (-> (kv/make-in-memory-kvstore)
                     (kv/restore (kv/tx-log kv1)))]

         (kv/snapshot kv1) => (kv/snapshot kv2)))




(facts "about Tx-Log protocol: flush-tx-log must be able to flush the given transactions"

       ;; flushing a tx-log which hasn't changed should empty it
       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))

             txlog (kv/tx-log kv1)

             kvf (kv/flush-tx-log kv1 txlog)]

         (kv/tx-log kvf) => [])



       ;; flushing a tx-log which has changed should remove
       ;; only the given tx
       ;; this test isn't nice as expose inner structure
       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))

             txlog (kv/tx-log kv1)

             kv2 (-> kv1
                     (kv/set "s1" "key1" "v4"))

             kvf (kv/flush-tx-log kv2 txlog)]

         (kv/tx-log kvf) => [["s1" 4 {"key1" "v4"}]]))



(facts "about Tx-Log protocol: restore must flushed the tx-log after restore"

       ;; mix instructions restore
       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))

             kv2 (-> (kv/make-in-memory-kvstore)
                     (kv/restore (kv/tx-log kv1)))]

         (kv/tx-log kv2) => []))





(facts "about Tx-Log protocol: versions must be kept per sourceId to allow being recombined after resharding"

       ;; mix instructions restore
       (let [kv0 (kv/make-in-memory-kvstore)
             kv1 (-> kv0
                     (kv/set "s1" "key1" "v1")
                     (kv/set "s2" "key1" "vB")
                     (kv/set "s1" "key1" "v2")
                     (kv/del "s1" "key1")
                     (kv/set "s2" "key2" "vC"))

             kv2 (-> (kv/make-in-memory-kvstore)
                     (kv/restore (kv/tx-log kv1)))]

         (->> (kv/tx-log kv1)
              (map (juxt first second))
              (into {})) => {"s1" 3 "s2" 2}

         (->> (:data kv2)
              :version) => {"s1" 3 "s2" 2}))
