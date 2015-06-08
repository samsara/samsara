(ns moebius.kv-test
  (:require [moebius.kv :as kv]
            [midje.sweet :refer :all])
  (:import [moebius.kv KV InMemoryKVstore]))


(def kvstore (kv/make-in-memory-kvstore))


(facts "about KV protocols"

       (fact "I should be able to set a property with any value"

             ;; assertion                             expected result
             (-> kvstore
                 (kv/set "s1" "key1" "v1")
                 (kv/get "s1" "key1"))         =>       "v1"

             (-> kvstore
                 (kv/set "s1" "key1" 1)
                 (kv/get "s1" "key1"))         =>       1

             (-> kvstore
                 (kv/set "s1" "key1" true)
                 (kv/get "s1" "key1"))         =>       true

             (-> kvstore
                 (kv/set "s1" "key1" :a-value)
                 (kv/get "s1" "key1"))         =>       :a-value
             )


       (fact "properties are independent across sourceId"

             ;; assertion                             expected result
             (-> kvstore
                 (kv/set "s1" "key1" "v1")
                 (kv/set "s2" "key1" "v2")
                 (kv/get "s1" "key1"))         =>       "v1"

             (-> kvstore
                 (kv/set "s1" "key1" "v1")
                 (kv/set "s2" "key1" "v2")
                 (kv/get "s2" "key1"))         =>       "v2"

                 ))
