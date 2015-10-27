(ns samsara-core.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [moebius.kv :as kv]
            [samsara.utils :refer [from-json to-json]]
            [taoensso.timbre :as log]
            [clj-kafka.producer :as kp]))

(defn gzip-input-stream-wrapper [file]
  (let [in (io/input-stream file)]
    (if (.endsWith (.toString file) ".gz")
      (java.util.zip.GZIPInputStream. in) in)))


(defn gzip-output-stream-wrapper [file]
  (let [out (io/output-stream file)]
    (if (.endsWith (.toString file) ".gz")
      (java.util.zip.GZIPOutputStream. out) out)))


(defn write-txlog-to-file [kvstore tx-file]
  (with-open [wrtr (io/writer (gzip-output-stream-wrapper tx-file))]
    (doseq [tx (kv/tx-log kvstore)]
      (.write wrtr (str (to-json tx) \newline)))))


(defn load-txlog-from-file
  ([tx-file]
   (load-txlog-from-file (kv/make-in-memory-kvstore)  tx-file))
  ([kvstore tx-file]
   (with-open [rdr (io/reader (gzip-input-stream-wrapper tx-file))]
     (->> (line-seq rdr)
          (map from-json)
          (kv/restore kvstore)))))



(defn send-txlog-to-topic
  "it pushed the txlog of a kv-store into a kafka topic.
   This is useful to bootstrap a topic with some data."
  [brokers topic txlog]
  (let [prodx  (kp/producer
                {"metadata.broker.list" brokers
                 "request.required.acks"  "-1"
                 "producer.type" "sync"
                 "serializer.class" "kafka.serializer.StringEncoder"
                 "partitioner.class" "kafka.producer.DefaultPartitioner"})]
    (let [->msg (fn [[key :as tx]] (kp/message topic (str key) (to-json tx)))]
      (kp/send-messages prodx (map ->msg txlog)))))



(defn send-kvstore-to-topic
  "it pushed the txlog of a kv-store into a kafka topic.
   This is useful to bootstrap a topic with some data."
  [brokers topic kvstore]
  (send-txlog-to-topic brokers topic (kv/tx-log kvstore)))



(defn bootstrap-dimension-from-file
  [brokers topic file]
  (log/info "Reading txlog from file:" file "and sending to topic:" topic "via:" brokers)
  (with-open [rdr (io/reader (gzip-input-stream-wrapper file))]
    (doseq [log-batch (partition-all 5000 (map from-json (line-seq rdr)))]
      (send-txlog-to-topic brokers topic log-batch))))
