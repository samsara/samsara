(ns qanal.transform
  (:require [clj-time
             [coerce :as tc]
             [format :as f]]))


(defmulti topic-transducer
  (juxt :type :indexing-strategy (comp not nil? :id-field)))


(defmethod topic-transducer [:river nil false] [topic-cfg]
  identity)


(defmethod topic-transducer [:plain :simple false]
  [{:keys [index doc-type]}]
  (fn [doc]
    {:index  index
     :type   doc-type
     :source doc}))


(defmethod topic-transducer [:plain :simple true]
  [{:keys [index doc-type id-field]}]
  (let [idf (keyword id-field)]
    (fn [doc]
      {:index  index
       :type   doc-type
       :id     (idf doc)
       :source doc})))


(defmethod topic-transducer [:plain :simple true]
  [{:keys [index doc-type id-field]}]
  (let [idf (keyword id-field)]
    (fn [doc]
      {:index  index
       :type   doc-type
       :id     (idf doc)
       :source doc})))

(defmulti timestamp-extractor
  (fn [field format]
    (cond
      (= :system field)    :system
      (= :millis format)   :millis
      (= :iso-8601 format) :iso-8601
      :else                :custom)))


(defmethod timestamp-extractor :system [_ _]
  (fn [_] (System/currentTimeMillis)))



(defmethod timestamp-extractor :millis [field format]
  (let [tsf (keyword field)]
    (fn [doc] (tsf doc))))



(defmethod timestamp-extractor :iso-8601 [field format]
  (let [tsf (keyword field)
        tparser #(tc/to-long (f/parse (f/formatters :date-time) %))]
    (fn [doc] (tparser (tsf doc)))))



(defmethod timestamp-extractor :custom [field format]
  (let [tsf (keyword field)
        tparser #(tc/to-long (f/parse (f/formatter format) %))]
    (fn [doc] (tparser (tsf doc)))))



(defmethod topic-transducer [:plain :daily true]
  [{:keys [base-index doc-type id-field timestamp-field timestamp-field-format]}]
  (let [idf (keyword id-field)
        tsf (timestamp-extractor timestamp-field timestamp-field-format)
        idx-date-fmt #(str base-index (f/unparse (f/formatter "-YYYY-MM-dd") (tc/from-long %)))]
     (fn [doc]
      {:index  (idx-date-fmt (tsf doc))
       :type   doc-type
       :id     (idf doc)
       :source doc})))


(defmethod topic-transducer [:plain :daily false]
  [{:keys [base-index doc-type timestamp-field timestamp-field-format]}]
  (let [tsf (timestamp-extractor timestamp-field timestamp-field-format)
        idx-date-fmt #(str base-index (f/unparse (f/formatter "-YYYY-MM-dd") (tc/from-long %)))]
    (fn [doc]
      {:index  (idx-date-fmt (tsf doc))
       :type   doc-type
       :source doc})))
