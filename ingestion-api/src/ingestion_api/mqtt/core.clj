(ns ingestion-api.mqtt.core
  (:require [compojure.route :refer [files not-found]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST DELETE ANY context]]
            [clojure.string :refer [split trim lower-case]]
            [org.httpkit.server :refer [run-server accept on-close on-receive
                                        send! close]])
  (:require [ingestion-api.mqtt.handler :as handler])
  (:require [ingestion-api.mqtt.tcp :as tcp])
  (:import [org.httpkit.server AsyncChannel])
  (:gen-class))

;;;;;; HTTP Kit Sub protocol validation
;;;;;; Thanks https://gist.github.com/cgmartin/5880732

(defn origin-match? [origin-re req]
  (if-let [req-origin (get-in req [:headers "origin"])]
    (re-matches origin-re req-origin)))

(defn subprotocol? [proto req]
  (if-let [protocols (get-in req [:headers "sec-websocket-protocol"])]
    (some #{proto}
          (map #(lower-case (trim %))
               (split protocols #",")))))

(defmacro with-subproto-channel
  [request ch-name origin-re subproto & body]
  `(let [~ch-name (:async-channel ~request)]
     (if (:websocket? ~request)
       (if-let [key# (get-in ~request [:headers "sec-websocket-key"])]
         (if (origin-match? ~origin-re ~request)
           (if (subprotocol? ~subproto ~request)
             (do
               (.sendHandshake ~(with-meta ch-name {:tag `AsyncChannel})
                               {"Upgrade"    "websocket"
                                "Connection" "Upgrade"
                                "Sec-WebSocket-Accept"   (accept key#)
                                "Sec-WebSocket-Protocol" ~subproto})
               ~@body
               {:body ~ch-name})
             {:status 400 :body "missing or bad WebSocket-Protocol"})
           {:status 400 :body "missing or bad WebSocket-Origin"})
         {:status 400 :body "missing or bad WebSocket-Key"})
       {:status 400 :body "not websocket protocol"})))


;;;;;;;;;;;;;;;;;;;;;;;; Web Server
(defn show-landing-page [req] ;; ordinary clojure function, accepts a request map, returns a response map
  ;; return landing page's html string. Using template library is a good idea:
  ;; mustache (https://github.com/shenfeng/mustache.clj, https://github.com/fhd/clostache...)
  ;; enlive (https://github.com/cgrand/enlive)
  ;; hiccup(https://github.com/weavejester/hiccup)
  )

(defn ws-handler [request]
  (with-subproto-channel request channel #".*" "mqttv3.1"
    (on-close channel (fn [status] (println "channel closed: " status)))
    (on-receive channel (fn [data] ;; echo it back
                          (try
                            (when-let [resp (handler/mqtt-handler data)]
                              (send! channel resp))
                            (catch Throwable t
                              (do (.printStackTrace t)
                                  (close channel))))))))

(defroutes all-routes
  (GET "/" [] "Hello World")
  (GET "/mqtt" [] ws-handler)     ;; websocket
  (files "/static") ;; `public` folder
  (not-found "<p>Page not found.</p>")) ;; all other, return 404

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting MQTT server...")
  (reset! server (run-server #'all-routes {:port 9090}))
  (println "Starting TCP server at 10010")
  (tcp/start-tcp-server 10010))
