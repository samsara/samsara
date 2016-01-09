(ns samsara-builtin-modules.ip-geo-location
  (:require [moebius.core :refer :all])
  (:require [ip-geoloc.core :as geo]))

(def cfg
  {:database-file "/tmp/GeoLite2-City.mmdb"})


(defn init-provider! [cfg]
  (geo/init-provider! cfg)
  (geo/start-provider!))


(defenrich ip-geo-locate [{:keys [clientIp] :as event}]
  (when clientIp
    (let [{:keys [city country latitude longitude]}
          (geo/geo-lookup clientIp)]
      (-> event
          (inject-as :city      city)
          (inject-as :country   country)
          (inject-as :latitude  latitude)
          (inject-as :longitude longitude)))))


(comment

  (init-provider! cfg)

  (geo/geo-lookup provider "8.8.8.8")
  )
