(ns ingestion-api.route
  (:require [ingestion-api.route-util :refer [gzip-req-wrapper]])
  (:require [reloaded.repl :refer [system]])
  (:require [samsara.trackit :refer [track-distribution]])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [ring.util.response :refer :all :exclude [not-found]]
            [clojure.pprint :refer [pprint]])
  (:require [ingestion-api.status :refer [change-status! is-online?]]
            [ingestion-api.events :refer [send! is-invalid? inject-receivedAt
                                          inject-publishedAt]]))


(defn not-found []
   (rfn request
       {:status 404 :body {:status "ERROR" :message "Not found"}}))

(defn warn-when-missing-header [postingTimestamp]
  (when-not postingTimestamp
    {:status "OK"
     :warning "For completeness, please provide the 'X-Samsara-publishedTimestamp' header."}))


(defn- to-long
  "Convert a string to a number when possible, nil otherwhise."
  [^String num]
  (when num
    (try
      (Long/parseLong num)
      (catch Exception x nil))))


(defroutes app-routes

  (context "/v1" []

           (POST  "/events"   {events :body
                               {postingTimestamp "x-samsara-publishedtimestamp"} :headers}
                  (if-let [errors (is-invalid? events)]
                    {:status 400 :body (map #(if % % "OK") errors)}
                    (do
                      (track-distribution "ingestion.payload.size" (count events))
                      (send! events
                             postingTimestamp
                             (-> system :http-server :backend :backend))
                      {:status 202
                       :body (warn-when-missing-header postingTimestamp)})))

           (GET "/api-status" []
                {:status (if (is-online?) 200 503)
                 :body {:status (if (is-online?) "online" "offline")}})

           (PUT "/api-status" {{new-status :status} :body}
                (if-not (= :error (change-status! new-status))
                  {:status 200 :body nil}
                  {:status 400 :body nil})))

  (not-found))


(defn catch-all [handler]
  (fn [req]
    (try (handler req)
         (catch Exception x
           (println "-----------------------------------------------------------------------")
           (println (java.util.Date.) "[ERR!] -- " x)
           (.printStackTrace x)
           (pprint req)
           (println "-----------------------------------------------------------------------")
           {:status  500 :headers {} :body  nil}))))


(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (gzip-req-wrapper)
      catch-all))
