(ns samsara-builtin-modules.ip-geo-location
  (:require [moebius.core :refer :all])
  (:require [ip-geoloc.core :as geo]))

(def cfg
  {:db-file "/tmp/GeoLite2-City.mmdb"})


(def ^:dynamic *provider* nil)


(defn init-provider [file]
  (geo/init-provider :max-mind2 file))


(defn init-provider! [file]
  (alter-var-root #'*provider* (constantly (init-provider file))))


(defenrich ip-geo-locate [{:keys [clientIp] :as event}]
  (when clientIp
    (let [{:keys [city country latitude longitude]}
          (geo/geo-lookup provider clientIp)]
      (-> event
          (inject-as :city      city)
          (inject-as :country   country)
          (inject-as :latitude  latitude)
          (inject-as :longitude longitude)))))


(comment

  (init-provider! (:db-file cfg))

  (geo/geo-lookup provider "8.8.8.8")
  )
