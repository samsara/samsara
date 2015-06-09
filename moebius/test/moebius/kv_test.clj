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



(facts "about KV protocol: if a key hasn't been set or remove should return `nil`"

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





(facts "about Tx-Log protocol: updates must be recorded in tx-log"

       ;; assertion                             expected result
       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/tx-log)
           count)

       =>       2



       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/tx-log))

       => [[1 ["s1" "key1"] "v1"]
           [2 ["s1" "key1"] "v2"]]



       (-> (kv/make-in-memory-kvstore)
           (kv/set "s1" "key1" "v1")
           (kv/set "s1" "key1" "v2")
           (kv/set "s1" "key1" nil)
           (kv/tx-log))

       => [[1 ["s1" "key1"] "v1"]
           [2 ["s1" "key1"] "v2"]
           [3 ["s1" "key1"] nil]]



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
