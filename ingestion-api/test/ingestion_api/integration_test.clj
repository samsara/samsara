(ns ingestion-api.integration-test
  (:refer-clojure :exclude [send])
  (:require [ingestion-api.backend.backend-protocol :refer [send]])
  (:require [ingestion-api.system :refer [ingestion-api-system]]
            [com.stuartsierra.component :as component])
  (:require [clj-http.client :as http])
  (:require [midje.sweet :refer :all])
  (:import  [ingestion_api.backend.backend_protocol EventsQueueingBackend]))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;       ---==| C R E A T E   A   T E S T I N G   B A C K E N D |==----       ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;
;; This backend is for testing purposes, it just prints
;; the events to the stdout.
;;
(defprotocol TestingBackendRetrieve
  "Backend queueing system abstraction where events are sent to.
  The purpose of this protocol is to abstract the backend
  and enable a pluggable architecture where different backends
  can be used."

  (fetch-events [this]
    "return the events which have been sent to the Testing backend"))

(deftype TestingBackend [data-atom]
  EventsQueueingBackend

  (send [_ events]
    (swap! data-atom #(apply conj % events)))

  TestingBackendRetrieve

  (fetch-events [_]
    @data-atom)
  )



(defn make-testing-backend
  "Create a in memory backend for testing purposes"
  []
  (TestingBackend. (atom [])))


(comment
  (def t (make-testing-backend))


  (send t [{:a 1}])

  (fetch-events t)

  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ---==| T E S T I N G   T O O L S |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn random-port []
  (+ 10000 (rand-int 25000)))

(defn make-test-system []
  (let [config {:server {:port (random-port)}
                :admin-server {:port (random-port)}
                :mqtt   {:port (random-port) :enabled true}
                ;; will be then replaced with testing one
                :backend {:type :console :pretty? true}}]
    (-> (ingestion-api-system config)
        (assoc-in [:backend :config] {:type :testing})
        (assoc-in [:backend :backend] (make-testing-backend)))))


(defn HTTP-REQUEST [rfn {:keys [host port version path body headers]
                         :or {host "localhost"
                              port 9000
                              version "v1"
                              path "/"
                              headers {}}}]
  (rfn (str "http://" host ":" port "/" version path)
       {:accept :json :as :json
        :coerce :always
        :headers headers
        :content-type :json :form-params body
        :throw-exceptions false}))

(defn GET [& {:keys [host port version path body]
              :or {host "localhost"
                   port 9000
                   version "v1"
                   path "/"} :as req}]
  (HTTP-REQUEST http/get req))


(defn PUT [& {:keys [host port version path body]
              :or {host "localhost"
                   port 9000
                   version "v1"
                   path "/"} :as req}]
  (HTTP-REQUEST http/put req))


(defn POST [& {:keys [host port version path body]
               :or {host "localhost"
                    port 9000
                    version "v1"
                    path "/"} :as req}]
  (HTTP-REQUEST http/post req))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;         ---==| H T T P   I N T E G R A T I O N   T E S T S |==----         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(fact "HTTP POST /events: single event with X-Samsara-publishedTimestamp header" :integration

  (let [system (component/start (make-test-system))
        port   (-> system :http-server :port)
        admin  (-> system :admin-server :port)]

    ;; posting single event
    (POST :path "/events" :port port :headers {"X-Samsara-publishedTimestamp" 2}
          :body [{:timestamp 1 :eventName "A" :sourceId "A"}])

    => (contains {:status 202})

    ;; checking backend
    (-> system :backend :backend fetch-events)
    => (just [(just {:timestamp 1 :eventName "A" :sourceId "A"
                     :publishedAt 2 :receivedAt integer?})])

    (component/stop system)))




(fact "HTTP POST /events: single event without header, expect warning" :integration

      (let [system (component/start (make-test-system))
            port   (-> system :http-server :port)
            admin  (-> system :admin-server :port)]

        ;; posting single event, expected warning message
        (POST :path "/events" :port port
              :body [{:timestamp 1 :eventName "A" :sourceId "A"}])

        => (contains
            {:status 202
             :body {:status "OK",
                    :warning "For completeness, please provide the 'X-Samsara-publishedTimestamp' header."}})

        ;; checking backend
        (-> system :backend :backend fetch-events)
        => (just [(just {:timestamp 1 :eventName "A" :sourceId "A"
                         :receivedAt integer?})])

        (component/stop system)))



(fact "HTTP POST /events: invalid events with X-Samsara-publishedTimestamp header" :integration

      (let [system (component/start (make-test-system))
            port   (-> system :http-server :port)
            admin  (-> system :admin-server :port)]

        ;; posting single event
        (POST :path "/events" :port port :headers {"X-Samsara-publishedTimestamp" 2}
              :body [{:timestamp 1 :eventName "A" :sourceId "A"} ;; valid
                     {:timestamp 2 :sourceId "A"}                ;; missing eventName
                     {:timestamp 3 :eventName "A"}               ;; missing sourceId
                     {:eventName "A" :sourceId "A"}              ;; missing timestamp
                     {:timestamp "5" :eventName "A" :sourceId "A"} ;; invalid timestamp
                     {:timestamp 6 :eventName "A" :sourceId "A"} ;; valid
                     ])

        => (contains {:status 400
                      :body (just ["OK"
                                   "{:eventName missing-required-key}"
                                   "{:sourceId missing-required-key}"
                                   "{:timestamp missing-required-key}"
                                   "{:timestamp (not (integer? \"5\"))}"
                                   "OK"])})

        ;; checking backend
        (-> system :backend :backend fetch-events)
        => []

        (component/stop system)))




(facts "HTTP PUT /api-status offline should work only from admin endpoint" :integration

      (let [system (component/start (make-test-system))
            port   (-> system :http-server :port)
            admin  (-> system :admin-server :port)]


        (fact "api initially should be online"
              (GET :path "/api-status" :port port)

              => (contains {:status 200
                            :body {:status "online"}}))


        (fact "put offline from client port isn't allowed"
              (PUT :path "/api-status" :port port
                   :body {:status "offline"})

              => (contains {:status 404}))

        (fact "api should be online even from the admin port"
              (GET :path "/api-status" :port admin)

              => (contains {:status 200
                            :body {:status "online"}}))


        (fact "put offline from admin endpoint should be allowed"
              (PUT :path "/api-status" :port admin
                   :body {:status "offline"})

              => (contains {:status 200}))

        (fact  "once offline the status should go to service unavailable 503"
               (GET :path "/api-status" :port port)
               => (contains {:status 503
                             :body {:status "offline"}}))

        (fact "also on the admin port"
              (GET :path "/api-status" :port admin)
              => (contains {:status 503
                            :body {:status "offline"}}))

        (fact "put online from admin endpoint should be allowed"
              (PUT :path "/api-status" :port admin
                   :body {:status "online"})

              => (contains {:status 200}))

        (fact "finally api should be online"
              (GET :path "/api-status" :port port)

              => (contains {:status 200
                            :body {:status "online"}}))

        (component/stop system)))
