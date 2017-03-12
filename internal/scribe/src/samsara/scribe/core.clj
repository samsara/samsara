(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json]
            [samsara.scribe.serializer.edn :as edn]
            [samsara.scribe.serializer.nippy :as nippy]
            [samsara.scribe.serializer.fressian :as fressian]
            [samsara.scribe.serializer.transit :as transit]
            [samsara.scribe.protocol :as scribe]))


(defmulti scribe :type)



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 2.38 µs
;;    - Execution time std-deviation  : 110.73 ns
;;    - Execution time lower quantile : 2.25 µs ( 2.5%)
;;    - Execution time upper quantile : 2.57 µs (97.5%)
;;
;;  - complex object roundtrip with all Clojure's types
;;    - Execution time mean           : 17.16 µs
;;    - Execution time std-deviation  :  1.26 µs
;;    - Execution time lower quantile : 15.04 µs ( 2.5%)
;;    - Execution time upper quantile : 19.38 µs (97.5%)
(defmethod scribe :json
  [config]
  (json/make-json-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 3.84 µs
;;    - Execution time std-deviation  : 347.84 ns
;;    - Execution time lower quantile : 3.55 µs ( 2.5%)
;;    - Execution time upper quantile : 4.27 µs (97.5%)
;;
;;  - complex object roundtrip with all Clojure's types
;;    - Execution time mean           : 60.83 µs
;;    - Execution time std-deviation  :  3.65 µs
;;    - Execution time lower quantile : 54.78 µs ( 2.5%)
;;    - Execution time upper quantile : 67.20 µs (97.5%)
(defmethod scribe :edn
  [config]
  (edn/make-edn-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 1.74 µs
;;    - Execution time std-deviation  : 128.70 ns
;;    - Execution time lower quantile : 1.51 µs ( 2.5%)
;;    - Execution time upper quantile : 1.96 µs (97.5%)
;;
;;  - complex object roundtrip with all Clojure's types
;;    - Execution time mean           : 17.76 µs
;;    - Execution time std-deviation  :  1.02 µs
;;    - Execution time lower quantile : 16.32 µs ( 2.5%)
;;    - Execution time upper quantile : 19.62 µs (97.5%)
(defmethod scribe :nippy
  [config]
  (nippy/make-nippy-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 5.30 µs
;;    - Execution time std-deviation  : 483.02 ns
;;    - Execution time lower quantile : 4.65 µs ( 2.5%)
;;    - Execution time upper quantile : 6.40 µs (97.5%)
;;
;;  - complex object roundtrip with all Clojure's types
;;    - Execution time mean           : 41.55 µs
;;    - Execution time std-deviation  :  2.58 µs
;;    - Execution time lower quantile : 37.41 µs ( 2.5%)
;;    - Execution time upper quantile : 46.07 µs (97.5%)
(defmethod scribe :fressian
  [_]
  (fressian/make-fressian-scribe))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 9.01 µs
;;    - Execution time std-deviation  : 355.23 ns
;;    - Execution time lower quantile : 8.67 µs ( 2.5%)
;;    - Execution time upper quantile : 9.82 µs (97.5%)
;;
;;  - complex object roundtrip with all Clojure's types
;;    - Execution time mean           : 28.16 µs
;;    - Execution time std-deviation  :  1.21 µs
;;    - Execution time lower quantile : 26.44 µs ( 2.5%)
;;    - Execution time upper quantile : 30.47 µs (97.5%)
(defmethod scribe :transit
  [config]
  (transit/make-transit-scribe (dissoc config :type)))
