(ns samsara.scribe.core
  (:require [samsara.scribe.serializer.json :as json]
            [samsara.scribe.serializer.edn :as edn]
            [samsara.scribe.serializer.nippy :as nippy]
            [samsara.scribe.serializer.fressian :as fressian]
            [samsara.scribe.serializer.transit :as transit]))


(defmulti scribe :type)



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 2.38 µs
;;    - Execution time std-deviation  : 110.73 ns
;;    - Execution time lower quantile : 2.25 µs ( 2.5%)
;;    - Execution time upper quantile : 2.57 µs (97.5%)
(defmethod scribe :json
  [config]
  (json/make-json-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 3.84 µs
;;    - Execution time std-deviation  : 347.84 ns
;;    - Execution time lower quantile : 3.55 µs ( 2.5%)
;;    - Execution time upper quantile : 4.27 µs (97.5%)
(defmethod scribe :edn
  [config]
  (edn/make-edn-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 1.74 µs
;;    - Execution time std-deviation  : 128.70 ns
;;    - Execution time lower quantile : 1.51 µs ( 2.5%)
;;    - Execution time upper quantile : 1.96 µs (97.5%)
(defmethod scribe :nippy
  [config]
  (nippy/make-nippy-scribe (dissoc config :type)))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean : 5.30 µs
;;    - Execution time std-deviation : 483.02 ns
;;    - Execution time lower quantile : 4.65 µs ( 2.5%)
;;    - Execution time upper quantile : 6.40 µs (97.5%)
(defmethod scribe :fressian
  [_]
  (fressian/make-fressian-scribe))



;; Performances:
;;  - simple object roundtrip `{:test 1}`
;;    - Execution time mean           : 9.01 µs
;;    - Execution time std-deviation  : 355.23 ns
;;    - Execution time lower quantile : 8.67 µs ( 2.5%)
;;    - Execution time upper quantile : 9.82 µs (97.5%)
(defmethod scribe :transit
  [config]
  (transit/make-transit-scribe (dissoc config :type)))
