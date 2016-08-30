(ns samsara.qanal.coordinator.zookeeper
  (:require [curator
             [discovery :as cd]
             [framework :as cf]
             [leader :as cl]]
            [samsara.qanal.coordinator.coordinator-protocol :refer [Coordinator]]
            [safely.core :refer [safely]]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [org.apache.curator.framework.recipes.cache NodeCache NodeCacheListener]))

(def ^:static LEADER-LOOP-SLEEP (* 30 1000))

(def ^:static BASE-PATH "/samsara")

(def ^:static SERVICE-NAME "qanal")

(def ^:static LEADERSHIP-PATH (str BASE-PATH "/" SERVICE-NAME "_leader" ))

(def ^:static JOBS-PATH (str BASE-PATH "/" SERVICE-NAME "_jobs"))


(def ^:const DEFAULT-STATE {:running false
                            :zk-cli nil
                            :discovery nil
                            :service-cache nil
                            :selector nil
                            :node-cache nil})


(defn create-cli [zk-connect]
  (let [cli (cf/curator-framework zk-connect)]
    (.start cli)
    cli))

(defn register-service [curator-cli id]
  (let [instance (cd/service-instance SERVICE-NAME
                                      "{scheme}://{address}:{port}"
                                      nil
                                      :id id)
        discovery (cd/service-discovery curator-cli instance :base-path BASE-PATH)]
    (.start discovery)
    discovery))

(defn watch-other-services [discovery]
  (let [service-cache (cd/service-cache discovery SERVICE-NAME)]
    (.start service-cache)
    service-cache))

(defn compete-for-leadership [curator-cli id won-leadership-fn]
  (let [selector (cl/leader-selector curator-cli LEADERSHIP-PATH won-leadership-fn
                                     :participant-id id)]
    (.start selector)
    selector))

(defn watch-for-jobs [zk-cli id new-jobs-fn]
  (let [node-cache (NodeCache. zk-cli (str JOBS-PATH "/" id))]
    (.start node-cache)
    (-> node-cache
        (.getListenable)
        (.addListener (reify NodeCacheListener
                        (nodeChanged [this]
                          (try
                            (if-let [ch-d (.getCurrentData node-cache)]
                              (let [data-str (-> ch-d
                                                 (.getData)
                                                 (String.))]
                                (new-jobs-fn data-str))
                              (new-jobs-fn nil))
                            (catch Exception e
                              ;; catching and throwing because curator will internally swallow
                              ;; these exceptions
                              (log/error "Exception occurred whilst informing worker of change. Due to"
                                         (.getMessage e))
                              (throw e)))))))
    node-cache))

(defn distribute
  "Evenly distributes the data into the given number of buckets.
   By \"Evenly\", I mean either all the buckets will have the same amount or
   there will only be a difference of one between any amount in any bucket

  e.g
  (distribute 5 (range 10))
  ; => [[0 5] [1 6] [2 7] [3 8] [4 9]]

  (distribute 5 (range 9))
  ; => [[0 5] [1 6] [2 7] [3 8] [4]]

  (distribute 5 (range 8))
  ; => [[0 5] [1 6] [2 7] [3] [4]]

  "
  [sz coll]
  (->> coll
       (partition sz sz (repeat nil))
       (apply map vector)
       (map (partial filter identity))))

(defn rebalance-jobs [qanal-ids qanal-jobs]
  (if (some #(= 0 (count %)) [qanal-ids qanal-jobs])
    {}
    (let [jobs-buckets (distribute (count qanal-ids) qanal-jobs)]
      (zipmap qanal-ids jobs-buckets))))

(defn cluster-changed? [assigned-jobs jobs qanal-ids]
  (let [current-ids (into #{} (keys assigned-jobs))
        new-ids (into #{} qanal-ids)
        current-jobs (into #{} (apply concat (vals assigned-jobs)))
        new-jobs (into #{} jobs)
        equal-size? #(= (count %1) (count %2))
        equal-content? #(every? %1 %2)
        the-same? #(and (equal-size? %1 %2) (equal-content? %1 %2))]

    (or (not (the-same? current-ids new-ids))
        (not (the-same? current-jobs new-jobs)))))

(defn display-assigned-jobs [assigned-jobs]
  (let [max-map {:max-id 0 :max-jobs 0}
        reduce-fn (fn [m [id jobs]]
                    (-> m
                        (update :max-id max (count (str id)))
                        (update :max-jobs max (count (str jobs)))))
        max-map (reduce reduce-fn max-map assigned-jobs)
        id-len (+ (:max-id max-map) 2)
        jobs-len (+ (:max-jobs max-map) 2)
        format-str (str "Qanal ID -> %-" id-len "s JobCount -> %s Jobs -> %-" jobs-len "s\n")]
    (log/info
     (with-out-str
       (printf "\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>> Qanal Assigned Jobs <<<<<<<<<<<<<<<<<<<<<<<<<<<<\n")
       (run! (fn [[id jobs]] (printf (format format-str id (count jobs) jobs))) assigned-jobs)
       (printf ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n")))))

(defn assign-jobs
  [new-jobs curator-cli]
  (when (.. curator-cli (checkExists) (forPath JOBS-PATH))
    (.. curator-cli (delete) (deletingChildrenIfNeeded)(forPath JOBS-PATH)))
  (run! (fn [[qanal-id qanal-job]]
          (let [qanal-job-path (str JOBS-PATH "/" qanal-id)
                qanal-job-data (.getBytes (str qanal-job))]
            (.. curator-cli (create) (creatingParentsIfNeeded) (forPath qanal-job-path qanal-job-data))))
        new-jobs)
  (display-assigned-jobs new-jobs))

(defn generate-leader-fn [worker service-cache sleep]
  (fn [curator-cli p-id]
    ;; Catching and re-throwing because curator will internally quietly handle
    ;; these exceptions, thus it is hard to know when our code is causing exceptions
    ;; NOTE: The thread that runs this function will be interrupted when leadership is lost.
    (safely
     (log/info "Gained Leadership. Participation-id:" p-id)
     (loop [new-jobs  (.all-jobs worker)
            instances (cd/instances service-cache)
            assigned-jobs (atom {})]
       (let [qanal-ids (mapv (fn [i] (.getId i)) instances)]
         (when (cluster-changed? @assigned-jobs new-jobs qanal-ids)
           (log/info "Qanal Cluster Change: Rebalancing jobs across cluster")
           (log/debug "Qanal Cluster Change: Detected IDs->" qanal-ids
                      "Detected Jobs->" new-jobs)
           (log/debug "Qanal Cluster Change: Existing IDs->" (keys @assigned-jobs)
                      "Existing Jobs->" (vals @assigned-jobs))
           (let [balanced-jobs (rebalance-jobs qanal-ids new-jobs)]
             (assign-jobs balanced-jobs curator-cli)
             (reset! assigned-jobs balanced-jobs)
             (log/info "Qanal Cluster Change: Rebalanced all jobs")))
         (Thread/sleep sleep)
         (recur (.all-jobs worker)
                (cd/instances service-cache)
                assigned-jobs)))
     :on-error
     :message (str "Lost Leadership. Participation-id:" p-id))))

(defn generate-worker-fn [worker]
  (fn [new-jobs]
    (.stop worker)
    (when new-jobs
      (.start worker new-jobs))))

(defn- complete-startup [id zk-connect worker]
  (let [zk-cli (create-cli zk-connect)
        discovery (register-service zk-cli id)
        service-cache (watch-other-services discovery)
        selector (compete-for-leadership zk-cli id (generate-leader-fn
                                                     worker service-cache LEADER-LOOP-SLEEP))
        node-cache (watch-for-jobs zk-cli id (generate-worker-fn worker))]

    (assoc DEFAULT-STATE
           :running true
           :zk-cli zk-cli
           :discovery discovery
           :service-cache service-cache
           :selector selector
           :node-cache node-cache)))

(defn- complete-shutdown [{:keys [zk-cli discovery service-cache selector node-cache]} worker]
  (.stop worker)
  (.close node-cache)
  (.close selector)
  (.close service-cache)
  (.close discovery)
  (.close zk-cli)
  DEFAULT-STATE)


(deftype ZookeeperCoordinator [id zk-connect worker state]
  Coordinator

  (start [this]
    (swap! state
           (fn [{:keys [running] :as s}]
             (if running
               s
               (complete-startup id zk-connect worker)))))

  (stop [this]
    (swap! state
           (fn [{:keys [running] :as s}]
             (if running
               (complete-shutdown worker)
               s)))))

;;use type hint for worker ?
(defn create-zk-coordinator [id zk-connect worker]
  (->ZookeeperCoordinator id zk-connect worker (atom DEFAULT-STATE)))
