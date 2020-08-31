(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
    [leihs.admin.resources.groups.back :as groups]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))


(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_groups)
      (sql/merge-where
        [:= :entitlement_groups_groups.entitlement_group_id
         :entitlement_groups.id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_users)
      (sql/merge-where
        [:= :entitlement_groups_users.entitlement_group_id
         :entitlement_groups.id])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_direct_users )
      (sql/merge-where
        [:= :entitlement_groups_direct_users.entitlement_group_id
         :entitlement_groups.id])))


(def entitlements-count
  (-> (sql/select :%count.*)
      (sql/from :entitlements )
      (sql/merge-where
        [:= :entitlements.entitlement_group_id
         :entitlement_groups.id])))

(defn entitlement-groups-query [inventory-pool-id]
  (-> (sql/select :*)
      (sql/from :entitlement_groups)
      (sql/merge-where
        [:= :entitlement_groups.inventory_pool_id inventory-pool-id])
      (sql/order-by :name)
      (sql/merge-select [direct-users-count :direct_users_count])
      (sql/merge-select [entitlements-count :entitlements_count])
      (sql/merge-select [groups-count :groups_count])
      (sql/merge-select [users-count :users_count])))

(defn entitlement-groups
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx :as request}]
  {:body
   {:entitlement-groups
    (->> inventory-pool-id
         entitlement-groups-query
         sql/format
         (jdbc/query tx))}})


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def groups-path (path :inventory-pool-entitlement-groups
                       {:inventory-pool-id ":inventory-pool-id"}))


(def routes
  (cpj/routes
    (cpj/GET groups-path  [] #'entitlement-groups)))

; "/admin/inventory-pools/8bd16d45-056d-5590-bc7f-12849f034351/entitlement-groups/7feff132-a5ee-42c4-af24-2d0dca47bf47/groups/bd739b13-04d3-4a96-bba2-5a26048e2986",
; "/admin/inventory-pools/:inventory-pool-id/entitlement-groups/:entitlement-group-id/groups/:group-id"

;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
