(ns ingestion-api.docker
  (:require [taoensso.timbre :as log])
  (:require [clojure.string :refer [upper-case join]]))

;;
;; TODO: this will be replaced by synapse as a library
;;

(defn- grep-env
  "filter the environment variables and keeps those whose key
  matches the given pattern"
  [env key-pattern]
  (filter
   (comp
    (partial re-matches
             (re-pattern
              (upper-case key-pattern)))
    first) env))


(defn- get-env []
  (into {} (System/getenv)))


(defn docker-link
  "Uses the environment variables to return the IP:PORT location of a linked
  container. If more than one container matches a comma-separeted string is returned"
  [{:keys [link port protocol]
                    :or {protocol "tcp"}}]
  (let [discovered
        (->> (grep-env (get-env)
                       (str "^" link "_PORT_" (or port ".*") "_" protocol "_ADDR$" ))
             (map second)
             (map #(str % (and port (str ":" port))))
             (join ","))]
    (log/debug "DOCKER-discovery: link to:" link "address discovered: '" discovered "'")
    discovered))


(defn docker-link-into
  "Uses the environment variables to return the IP:PORT location of a linked
  container. If more than one container matches a comma-separeted string is formed.
  The linked container address is then injected in the `config` map as the property
  specified in the `:to` option."
  [{:keys [link port protocol to] :as docker
                         :or {protocol "tcp"}} config]
  (assoc config to (docker-link docker)))
