(ns moebius.kv-test-check
  (:require [moebius.kv :as kv])
  (:require [midje.sweet :refer :all])
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def num-tests (Integer/getInteger "test-check.num-tests" 10))

(def instructions-gen
  (gen/elements [:get :set :del]))

(def write-instructions-gen
  (gen/elements [:set :del]))

(def sourceId-gen
  (gen/frequency [[7 (gen/elements ["dev1" "dev2" "dev3" "dev4"] )]
                  [2 (gen/such-that not-empty  gen/string-alphanumeric)]
                  [1 (gen/such-that not-empty  gen/string)]]))

(def simple-keys-gen
  (gen/one-of [gen/int (gen/not-empty gen/string) gen/keyword]))

(def composite-keys-gen
  (gen/not-empty (gen/vector simple-keys-gen)))

(def values-gen
  (gen/one-of [gen/int (gen/not-empty gen/string) gen/keyword]))


(def operation-gen
  (gen/tuple
   instructions-gen
   sourceId-gen
   (gen/one-of [simple-keys-gen composite-keys-gen])
   values-gen))


(def operation-list-gen
  (gen/vector operation-gen))

(defmulti apply-op (fn [k op] (first op)))

(defmethod apply-op :get [store [_ s k]]
  (kv/get store s k)
  store)

(defmethod apply-op :set [store [_ s k v]]
  (kv/set store s k v))

(defmethod apply-op :del [store [_ s k]]
  (kv/del store s k))



(fact "restore should recreate the exact state of the source" :test-check :slow
      (tc/quick-check
       num-tests
       (prop/for-all [ops operation-list-gen]
                     (let [new (kv/make-in-memory-kvstore)
                           k1  (reduce apply-op new ops)
                           k2  (kv/restore new (kv/tx-log k1))]
                       (= (kv/snapshot k1) (kv/snapshot k2)))))
      => (contains {:result true}))



(fact "writes on a particular key should hold just the last value" :test-check :slow
      (tc/quick-check
       num-tests
       (prop/for-all
        [ops (gen/vector
              (gen/tuple
               write-instructions-gen
               (gen/return "dev1")
               (gen/one-of [simple-keys-gen composite-keys-gen])
               values-gen))]
        (let [new (kv/make-in-memory-kvstore)
              k1  (reduce apply-op new ops)
              [o s k v]  (last ops)]
          (= (kv/get k1 s k) (if (= o :del) nil v)))))
      => (contains {:result true}))
