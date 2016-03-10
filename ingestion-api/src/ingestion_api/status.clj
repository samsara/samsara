(ns ingestion-api.status)
;;
;; This namespace handles the service /api-status.
;;
;; This is used to notify a front-end load balancer that a particular
;; instance in no longer in service.  Please note that this won't
;; affect in any way the behaviour or response of POST /events, which
;; means the POST /events will be still accepting incoming
;; requests. This behaviour is intended in this way.
;;
;; In production environments we experienced load balancers which
;; wouldn't remove a given instance even after several hours the
;; instance was removed from the load balancer set with the given
;; APIs.  Therefore the only way to have a graceful instance shutdown
;; is to make believe the load-balancer that the instance is offline.
;;
;; We achieve this by providing the load-balancer with a healthcheck
;; url (GET /api-status) and controlling the resulting status
;; (200 -> online, 503 -> offline).
;
(def status-online? (atom true))

(defn change-status!
  "It set the /api-status online or offline.
   This is used only to have a graceful way of removing
   a particular instance from a loadbalancer."
  [status]
  (case (name status)
    "online"  (swap! status-online? (constantly true))
    "offline" (swap! status-online? (constantly false))
    :error))

(defn is-online? []
  @status-online?)
