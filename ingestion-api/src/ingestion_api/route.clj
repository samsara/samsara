(ns ingestion-api.route
  (:require [ingestion-api.route-util :refer [gzip-req-wrapper]])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [ring.util.response :refer :all :exclude [not-found]]
            [clojure.pprint :refer [pprint]])
  (:require [ingestion-api.status :refer [change-status! is-online?]]
            [ingestion-api.events :refer [send! is-invalid? inject-receivedAt]]))


(defn not-found []
   (rfn request
       {:status 404 :body {:status "ERROR" :message "Not found"}}))


(defroutes app-routes

  (context "/v1" []

   (POST  "/events"   {events :body}
          (if-let [errors (is-invalid? events)]
            {:status 400 :body (map #(if % % "OK") errors)}
            (do
              (->> events
                   (inject-receivedAt (System/currentTimeMillis))
                   send!)
              {:status 202 :body nil})))

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
  (->
   (handler/site app-routes)
   (wrap-json-body {:keywords? true})
   (wrap-json-response)
   (gzip-req-wrapper)
   catch-all))
