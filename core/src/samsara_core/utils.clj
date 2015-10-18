(ns samsara-core.utils
  (:require [clojure.java.io :as io]
            [moebius.kv :as kv]
            [samsara.utils :refer [to-json from-json]]))


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
