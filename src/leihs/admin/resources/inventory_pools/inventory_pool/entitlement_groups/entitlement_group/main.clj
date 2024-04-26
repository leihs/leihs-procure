(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main :as entitlement-groups]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(defn query [inventory-pool-id entitlement-group-id]
  (-> entitlement-groups/base-query
      (sql/where
       [:= :entitlement_groups.inventory_pool_id inventory-pool-id])
      (sql/where [:= :entitlement_groups.id entitlement-group-id])))

(defn entitlement-group
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id :as route-params} :route-params
    tx :tx-next :as request}]
  (if-let [eg (->> (query inventory-pool-id entitlement-group-id)
                   sql-format
                   (jdbc-query tx)
                   first)]
    {:body eg}
    {:status 404}))

(defn routes [request]
  (case (:request-method request)
    :get (entitlement-group request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
