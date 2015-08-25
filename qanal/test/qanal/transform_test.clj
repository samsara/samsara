(ns qanal.transform-test
  (:require [clj-time
             [coerce :as tc]
             [format :as f]]
            [midje.sweet :refer :all]
            [qanal.transform :refer :all]))

(defn format-date [base-index millis]
  (str base-index (f/unparse (f/formatter "-YYYY-MM-dd") (tc/from-long millis))))

(def test-record
  {:id "abc123" :timestamp 1440511397267 :ts "2015-08-25T14:03:17.267Z"
   :eventName "test-event" :sourceId "device1"})


(facts "about `timestamp-extractor`: configured as :system"
       ;;
       ;; when configured to use :system time it should return the current time
       ;;
       (let [ts ((timestamp-extractor :system nil) {:timestamp 1})]
         (<= 0 (- (System/currentTimeMillis) ts) 100)) => true
       )



(facts "about `timestamp-extractor`: configured as field extraction in :millis"
       ;;
       ;; when configured to use a field it should extract its value is millis
       ;;
       ((timestamp-extractor "timestamp" :millis) {:timestamp 1}) => 1
       ((timestamp-extractor :timestamp  :millis) {:timestamp 100}) => 100
       ((timestamp-extractor :no-field   :millis) {:timestamp 100}) => nil
       )



(facts "about `timestamp-extractor`: configured as field extraction in :iso-8601 date"
       ;;
       ;; when configured to use a field it should extract its value is millis
       ;;
       ((timestamp-extractor "timestamp" :iso-8601)
        {:timestamp "2015-08-25T14:03:17.267-00:00"}) => 1440511397267

       ((timestamp-extractor "timestamp" :iso-8601)
        {:timestamp "2015-08-25T14:03:17.267Z"})     => 1440511397267

       ((timestamp-extractor :timestamp  :iso-8601)
        {:timestamp "2015-08-25"}) => (throws Exception)

       ((timestamp-extractor :no-field   :iso-8601)
        {:timestamp "2015-08-25T14:03:17.267-00:00"}) => (throws Exception)

       )




(facts "about `timestamp-extractor`: configured as field extraction in custom formatted date"
       ;;
       ;; when configured to use a field it should extract its value is millis
       ;;
       ((timestamp-extractor "timestamp" "YYYY-MM-dd")
        {:timestamp "2015-08-25"}) => 1440460800000

       ((timestamp-extractor "timestamp" "YYYY/MM/dd")
        {:timestamp "2015/08/25"}) => 1440460800000

        ((timestamp-extractor "timestamp" "YYYY/MM/dd")
         {:timestamp "2015-08-25"}) => (throws Exception)
       )



(facts "about `topic-transformer`: river format is a no-op"

       ;;
       ;; All transformer should eventually converge to the river format
       ;; if it is a river then it is a no-op
       ;;
       ((topic-transformer
         {:type :river :topic "test" :partitions :all})

        {:index "a-index" :type "a-type" :id "abc123"
         :source test-record})

       => {:index "a-index" :type "a-type" :id "abc123"
           :source test-record}
       )



(facts "about `topic-transformer`: plain format with simple index"

       ;;
       ;; All transformer should eventually converge to the river format
       ;; if the input format is a :plain non decorated record
       ;; then it needs to be wrapped in the river format
       ;;

       ;; with id field
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :simple :index "a-index" :doc-type "a-type"
          :id-field "id"})

        test-record)

       => {:index "a-index" :type "a-type" :id "abc123"
           :source test-record}


       ;; without id field
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :simple :index "a-index" :doc-type "a-type"
          })

        test-record)

       => {:index "a-index" :type "a-type"
           :source test-record}
       )



(facts "about `topic-transformer`: plain format with :daily index"

       ;;
       ;; All transformer should eventually converge to the river format
       ;; if the input format is a :plain non decorated record
       ;; then it needs to be wrapped in the river format
       ;; When using daily indexes then the index name must be built
       ;; with the given date
       ;;

       ;; with id field and system timestamp
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field :system :timestamp-field-format :millis :id-field "id"})

        test-record)

       => {:index  (format-date "a-index" (System/currentTimeMillis))
           :type   "a-type"
           :id     "abc123"
           :source test-record}


       ;; with id field and system field timestamp in millis
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field "timestamp" :timestamp-field-format :millis :id-field "id"})

        test-record)

       => {:index  "a-index-2015-08-25"
           :type   "a-type"
           :id     "abc123"
           :source test-record}


       ;; with id field and system field timestamp in millis
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field "ts" :timestamp-field-format :iso-8601
          :id-field "id"})

        test-record)

       => {:index  "a-index-2015-08-25"
           :type   "a-type"
           :id     "abc123"
           :source test-record}


       ;; with id field and system field timestamp in millis
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field "ts" :timestamp-field-format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
          :id-field "id"})

        test-record)

       => {:index  "a-index-2015-08-25"
           :type   "a-type"
           :id     "abc123"
           :source test-record}


       ;; with-out id field and system field timestamp in millis
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field "ts" :timestamp-field-format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
          })

        test-record)

       => {:index  "a-index-2015-08-25"
           :type   "a-type"
           :source test-record}


       ;; with id field which isn't present and system field timestamp in millis
       ((topic-transformer
         {:type :plain :topic "test" :partitions :all
          :indexing-strategy :daily :base-index "a-index" :doc-type "a-type"
          :timestamp-field "ts" :timestamp-field-format "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
          :id-field "NOT_PRESENT"})

        test-record)

       => {:index  "a-index-2015-08-25"
           :type   "a-type"
           :id     nil
           :source test-record}

       )
