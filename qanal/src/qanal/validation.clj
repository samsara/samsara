(ns qanal.validation
  (:require [qanal.utils :refer :all]
            [schema.core :as s]))


(defn validate-message
  "Validate format of incoming message"
  [schema msg]
  (when msg
    (safe-short nil (str "Invalid message format: " (prn-str msg))
                (s/validate schema msg))))


(defmulti topic-validator
  (juxt :type (comp :strategy :indexing)))


(def river-format
  "Validator schema for incoming messages"
  {(s/required-key :index)  s/Str
   (s/required-key :type)   s/Str
   (s/optional-key :id)     s/Str
   (s/required-key :source) {s/Any s/Any}})


(defmethod topic-validator [:river nil] [topic-cfg]
  (partial validate-message river-format))


(defmethod topic-validator [:plain :simple]
  [{{:keys [index doc-type id-field]} :indexing}]
  (partial validate-message {s/Any s/Any}))


(defmethod topic-validator [:plain :daily]
  [{{:keys [base-index doc-type id-field
            timestamp-field timestamp-field-format]} :indexing}]
  (if (= :system timestamp-field)

    (partial validate-message {s/Any s/Any})

    (let [fmt (if (= :millis timestamp-field-format) s/Int s/Str)]
      (partial validate-message
         {s/Any s/Any
          (s/required-key (keyword timestamp-field)) fmt}))))
