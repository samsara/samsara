(ns ingestion-api.input.route-util
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [compojure.core :refer [rfn]]
            [ns-tracker.core :refer [ns-tracker]]
            [taoensso.timbre :as log]
            [clojure.string :as str])
  (:import java.util.zip.GZIPInputStream))

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


(defn gzipped? [req]
  (= "gzip"
     (str/lower-case
      (get-in req [:headers "content-encoding"] ""))))



(defn- gunzip-request [{:keys [body] :as req}]
  (if body
    (-> req
        (update :body (fn [is] (GZIPInputStream. (io/input-stream is))))
        (dissoc-in [:headers "content-encoding"]))
    req))



(defn wrap-gzip-requests
  "if the request has content-enconding: gzip it automatically
   decompress the body"
  [handler]
  (fn [req]
    (if (gzipped? req)
      (handler (gunzip-request req))
      (handler req))))


(defn pretty-print-str
  [data]
  (with-out-str
    (pprint data)))



(defn catch-all [handler]
  (fn [req]
    (try (handler req)
         (catch Exception x
           (log/error x "Error processing request: \n" (pretty-print-str req))
           {:status  500 :headers {} :body  nil}))))


(defn not-found []
  (rfn request
       {:status 404 :body {:status "ERROR" :message "Not found"}}))

;;
;; adapted from https://github.com/ring-clojure/ring/blob/1.4.0/ring-devel/src/ring/middleware/reload.clj
;;
(defn wrap-reload
  "Reload namespaces of modified files before the request is passed to the
  supplied handler.
  Accepts the following options:
  :dirs - A list of directories that contain the source files.
          Defaults to [\"src\"]."
  {:arglists '([handler-fn] [handler-fn options])}
  [handler-fn & [options]]
  (let [source-dirs (:dirs options ["src"])
        modified-namespaces (ns-tracker source-dirs)]
    (fn [request]
      (doseq [ns-sym (modified-namespaces)]
        (require ns-sym :reload))
      ((handler-fn) request))))


(defn wrap-app
  [app-fn auto-reload]
  (if auto-reload
    (do
      (log/info "AUTO-RELOAD enabled!!! I hope you are in dev mode.")
      (wrap-reload app-fn))
    (app-fn)))
