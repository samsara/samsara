(ns ingestion-api.system
  (:require [com.stuartsierra.component :as component]
            [reloaded.repl :refer [system init start stop go reset]]
            [ingestion-api.components.http-server :as http]
            [ingestion-api.components.mqtt-server :as mqtt]))

(defn ingestion-api-system
  [config]
  (component/system-map
   :http-server (http/new-http-server config)
   :mqtt-server (mqtt/new-mqtt-server config)))

(def DEFAULT-CONFIG
  "Default configuration which will be merged with
  the user defined configuration."
  {:server {:port 9038 :auto-reload false}
   :mqtt   {:port 10038 :enabled true}

   :log   {:timestamp-pattern "yyyy-MM-dd HH:mm:ss.SSS zzz"}

   :backend  {:type :console :pretty? true}
   ;;:backend {:type :kafka :topic "ingestion" :metadata.broker.list "192.168.59.103:9092"}
   ;;:backend {:type :kafka-docker :topic "ingestion" :docker {:link "kafka.*" :port "9092" :to "metadata.broker.list"} }

   :tracking {:enabled false :type :console}
   })

;(reloaded.repl/set-init! #(ingestion-api-system DEFAULT-CONFIG))
;(go)
;(stop)




