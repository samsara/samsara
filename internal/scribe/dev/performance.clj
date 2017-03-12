(ns performance
  (:require [criterium.core :refer [bench quick-bench]])
  (:require [samsara.scribe.core :refer [scribe]]
            [samsara.scribe.protocol :as scribe]))



(defmacro roundtrip
  [s data]
  `(let [s# ~s]
     (scribe/read s# (scribe/write s# ~data))))



(defmacro roundtrip-bench
  [config data]
  `(let [s#    (scribe ~config)
         data# ~data]
     (println "------------------------------------------------------")
     (println "benchmark:" '~config "\nwith data:"  '~data)
     (println "------------------------------------------------------")
     (bench
      (roundtrip s# data#))))



(defmacro run-all-benchmarks
  [data]
  `(do
     (roundtrip-bench {:type :json}     ~data)
     (roundtrip-bench {:type :edn}      ~data)
     (roundtrip-bench {:type :nippy}    ~data)
     (roundtrip-bench {:type :fressian} ~data)
     (roundtrip-bench {:type :transit}  ~data)))



(comment

  (roundtrip-bench {:type :json}     {:test 1})
  (roundtrip-bench {:type :edn}      {:test 1})
  (roundtrip-bench {:type :nippy}    {:test 1})
  (roundtrip-bench {:type :fressian} {:test 1})
  (roundtrip-bench {:type :transit}  {:test 1})


  ;; simple object benchmark
  (run-all-benchmarks {:test 1})

  ;; complex object benchmark
  (run-all-benchmarks {:test 1
                       :foo "bar"
                       :keyword :quux
                       :symbol 'a
                       :set #{1 2 3}
                       :map {:foo "bar" :two 2}
                       :vector ["I" "am" "vector"]
                       :list '(1 4 6)
                       :valid? true
                       :date (java.util.Date.)
                       :character \space
                       :ratio 22/7
                       :integer 5000
                       :bigint 19931029N
                       :float -2.3
                       :bigdecimal 3.1415M})
  )
