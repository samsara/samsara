(ns ingestion-api.route-util
  (:require [clojure.java.io :as io])
  (:import (java.util.zip GZIPInputStream)
           (java.io InputStream
                    File
                    ByteArrayInputStream
                    ByteArrayOutputStream)))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))


(defn is-gzipped [req]
  (= "gzip"
     (clojure.string/lower-case
      (get-in req [:headers "content-encoding"] ""))))



(defn gunzip-request [req]
  (let [body (req :body)
        bout (ByteArrayOutputStream.)
        in  (GZIPInputStream. body)
        req (dissoc-in req [:headers "content-encoding"])]
    (io/copy in bout)
    (if (instance? InputStream body)
      (.close body))
    (assoc req :body (ByteArrayInputStream. (.toByteArray bout)))))



(defn gzip-req-wrapper [handler]
  (fn [req]
    (if (is-gzipped req)
      (handler (gunzip-request req))
      (handler req))))
