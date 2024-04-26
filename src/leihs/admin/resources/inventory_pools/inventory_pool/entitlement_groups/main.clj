(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.users.choose-core :as choose-user]
   [leihs.core.core :refer [presence]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_groups)
      (sql/where
       [:= :entitlement_groups_groups.entitlement_group_id
        :entitlement_groups.id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_users)
      (sql/where
       [:= :entitlement_groups_users.entitlement_group_id
        :entitlement_groups.id])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :entitlement_groups_direct_users)
      (sql/where
       [:= :entitlement_groups_direct_users.entitlement_group_id
        :entitlement_groups.id])))

(def entitlements-count
  (-> (sql/select :%count.*)
      (sql/from :entitlements)
      (sql/where
       [:= :entitlements.entitlement_group_id
        :entitlement_groups.id])))

(def base-query
  (-> (sql/select :*)
      (sql/from :entitlement_groups)
      (sql/order-by :name)
      (sql/select [direct-users-count :direct_users_count])
      (sql/select [entitlements-count :entitlements_count])
      (sql/select [groups-count :groups_count])
      (sql/select [users-count :users_count])))

(defn filter-for-including-user
  [query {{user-uid :including-user} :query-params-raw :as request}]
  (if-let [user-uid (presence user-uid)]
    (sql/where
     query
     [:exists
      (-> (choose-user/find-by-some-uid-query user-uid)
          (sql/select :true)
          (sql/join :entitlement_groups_users
                    [:= :entitlement_groups_users.entitlement_group_id
                     :entitlement_groups.id])
          (sql/where [:= :entitlement_groups_users.user_id :users.id]))])
    query))

(defn entitlement-groups-query
  [{{inventory-pool-id :inventory-pool-id} :route-params :as request}]
  (-> base-query
      (sql/where
       [:= :entitlement_groups.inventory_pool_id inventory-pool-id])
      (filter-for-including-user request)))

(defn entitlement-groups
  [{{inventory-pool-id :inventory-pool-id} :route-params
    tx :tx-next :as request}]
  {:body
   {:entitlement-groups
    (->> request
         entitlement-groups-query
         sql-format
         (jdbc-query tx))}})

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (entitlement-groups request)))

; "/admin/inventory-pools/8bd16d45-056d-5590-bc7f-12849f034351/entitlement-groups/7feff132-a5ee-42c4-af24-2d0dca47bf47/groups/bd739b13-04d3-4a96-bba2-5a26048e2986",
; "/admin/inventory-pools/:inventory-pool-id/entitlement-groups/:entitlement-group-id/groups/:group-id"

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
