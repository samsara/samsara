(ns moebius.pipeline-utils
  (:require [rhizome.viz :as viz]))



(defn- node-decorate
  "Internal node decorator function"
  [parent-id {:keys [moebius-name  moebius-wrapper moebius-type moebius-fns] :as node}]
  (let [id (keyword (str moebius-name "-" (System/identityHashCode node)))
        decor {:type moebius-type
               :node node
               :id id
               :parent-id parent-id
               :name moebius-name
               :fill (case moebius-wrapper :stateless "green" :stateful "orange")
               :style (case moebius-type :enrichment :oval :correlation :hexagon
                            :filtering :diamond :pipeline :invhouse)}]
    (if (= :pipeline moebius-type )
      (assoc decor
             :sub-nodes (map (partial node-decorate id) moebius-fns))
      decor)))



(defn view-pipeline-graph
  "Shows a graphical visualisation for the given pipeline"
  [pipeline]
  (let [;; extracting metadata from pipeline
        pipex (meta pipeline)

        ;; create map of edges as: nodes -> [ nodes ]
        graph (->> (node-decorate nil pipex)
                   (tree-seq :sub-nodes :sub-nodes)
                   (#(concat [{:id :start}] % [{:id :end}]))
                   (partition-all 2 1)
                   (map (fn [[{id1 :id}
                             {id2 :id}]]
                          [id1 [id2]]))
                   (into {}))

        ;; create map of nodes {:node-id node}
        nodes-index (->> (node-decorate nil pipex)
                         (tree-seq :sub-nodes :sub-nodes)
                         (#(concat [{:id :start :name "START" :fill "lightgrey"
                                     :shape :doublecircle}]
                                   %
                                   [{:id :end :name "END" :fill "lightgrey"
                                     :shape :doublecircle}]))
                         (map (juxt :id identity))
                         (into {}))

        ;; create map of parents nodes {:node-id :parent-node-id}
        nodes-parents (->> (node-decorate nil pipex)
                           (tree-seq :sub-nodes :sub-nodes)
                           (map (juxt :id :parent-id))
                           (into {}))]

    ;; now ready to build graph
    (viz/view-graph
     (keys graph) graph
     :node->descriptor
     (fn [n]
       (let [{:keys [name fill style]}(get nodes-index n)]
         {:label name :fillcolor fill :shape style :style :filled}))
     :node->cluster
     (fn [n]
       (let [{:keys [type]}(get nodes-index n)]
         (if (= :pipeline type) n (nodes-parents n))))
     :cluster->parent nodes-parents)))
