(ns ingestion-api.mqtt.tcp
  (:require [manifold.stream :as s]
            [manifold.deferred :as d]
            [ingestion-api.mqtt.domain.connack :as connack]
            [ingestion-api.mqtt.handler :as mqtt]
            [aleph.tcp :as tcp]))

;;;;;;;;; TCP Server

(defn do-safely
  "Calls a function safely by wrapping it within a try block.
   Returns nil when an exception is thrown."
  [f]
  (try (f) (catch Throwable t (do (.printStackTrace t) nil))))


(defn mqtt-handler
  "MQTT Handler for TCP"
  [s info]
  (d/loop []
    (d/chain
     ;; Take a message
     (s/take! s ::drained)
     ;; Call the actual mqtt-handler
     (fn [data]
       (if (identical? ::drained data)
         ::drained
         (do-safely #(mqtt/mqtt-handler data))))
     ;; Write the result
     (fn [result]
       (when-not (identical? ::drained result)
         (when-let [data result]
           (s/put! s data))
         (d/recur))))))


(defonce tcp-server (atom nil))

(defn start-tcp-server
  "Starts the TCP server at the given port"
  [port]
  (reset! tcp-server 
          (tcp/start-server mqtt-handler {:port port})))

(defn stop-tcp-server
  "Stops the TCP server"
  []
  (.close @tcp-server))







