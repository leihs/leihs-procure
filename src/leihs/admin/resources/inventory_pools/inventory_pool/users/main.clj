(ns leihs.admin.resources.inventory-pools.inventory-pool.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.set :refer [rename-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.shared :refer [default-query-params]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.seq :as seq]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query, delete! jdbc-delete!}]))

;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-effective-role [query inventory-pool-id role]
  (-> query
      (sql/join [:access_rights :far]
                [:and
                 [:= :far.user_id :users.id]
                 [:= :far.inventory_pool_id inventory-pool-id]])
      (sql/where [:in :far.role
                  (map name (roles/expand-to-hierarchy-up-and-include role))])))

(defn filter-for-none-role [query inventory-pool-id]
  (-> query
      (sql/where
       [:not [:exists (-> (sql/select :true)
                          (sql/from [:access_rights :far])
                          (sql/where [:= :far.inventory_pool_id inventory-pool-id])
                          (sql/where [:= :far.user_id :users.id]))]])))

(defn filter-by-role
  [query inventory-pool-id
   {query-params-raw :query-params-raw :as request}]
  (let [role (:role (merge default-query-params query-params-raw))]
    (case role
      "" query
      "any" query
      "none" (filter-for-none-role query inventory-pool-id)
      ("customer"
       "group_manager"
       "lending_manager"
       "inventory_manager") (filter-effective-role
                             query inventory-pool-id role))))

(defn suspension-join [query inventory-pool-id]
  (sql/join query :suspensions
            [:and
             [:= :suspensions.user_id :users.id]
             [:= :suspensions.inventory_pool_id inventory-pool-id]]))

(defn merge-aggreaged-role-to-query [query inventory-pool-id]
  (-> query
      (sql/left-join :access_rights
                     [:and
                      [:= :users.id :access_rights.user_id]
                      [:= :access_rights.inventory_pool_id inventory-pool-id]])
      (sql/select [:access_rights.role :role])))

(defn merge-direct-role-to-query [query inventory-pool-id]
  (-> query
      (sql/left-join :direct_access_rights
                     [:and
                      [:= :users.id :direct_access_rights.user_id]
                      [:= :direct_access_rights.inventory_pool_id inventory-pool-id]])
      (sql/select [:direct_access_rights.role :direct_role])))

;;; group access_rights ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-role-query [inventory-pool-id]
  (-> (sql/select [[:role_agg :group_access_rights.role]])
      (sql/from :group_access_rights)
      (sql/where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
      (sql/join :groups [:= :groups.id :group_access_rights.group_id])
      (sql/join :groups_users [:= :groups.id :groups_users.group_id])
      (sql/where [:= :groups_users.user_id :users.id])
      (sql/group-by :groups_users.user_id)))

(defn role-to-roles-map [ks data]
  (update-in data ks #(-> % roles/expand-to-hierarchy roles/roles-to-map)))

;;; users query ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-suspended-until-to-query
  ([query inventory-pool-id]
   (add-suspended-until-to-query query inventory-pool-id :users))
  ([query inventory-pool-id table]
   (-> query
       (sql/select [(-> (sql/select [[:raw " json_agg(to_json(suspensions.*))"]])
                        (sql/from :suspensions)
                        (sql/where [:= :suspensions.user_id [:. table :id]])
                        (sql/where [:= :suspensions.inventory_pool_id inventory-pool-id])) :suspension]))))

;;; suspension filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-subquery
  ([inventory-pool-id] (suspension-subquery inventory-pool-id :users))
  ([inventory-pool-id table]
   (-> (sql/select :true)
       (sql/from :suspensions)
       (sql/where [:= :suspensions.user_id [:. table :id]])
       (sql/where [:= :suspensions.inventory_pool_id inventory-pool-id])
       (sql/where [:raw "CURRENT_DATE <= suspensions.suspended_until"]))))

(defn filter-suspended
  ([query inventory-pool-id request]
   (filter-suspended query inventory-pool-id request :users))
  ([query inventory-pool-id request table]
   (case (-> request :query-params :suspension)
     "suspended" (sql/where
                  query
                  [:exists (suspension-subquery inventory-pool-id table)])
     "unsuspended" (sql/where
                    query
                    [:not [:exists (suspension-subquery inventory-pool-id table)]])
     query)))

;;; users query ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-query [inventory-pool-id request]
  (-> request
      users/users-query
      (sql/select [inventory-pool-id :inventory_pool_id])
      (filter-by-role inventory-pool-id request)
      (filter-suspended inventory-pool-id request)
      (merge-aggreaged-role-to-query inventory-pool-id)
      (merge-direct-role-to-query inventory-pool-id)
      (sql/select [(groups-role-query inventory-pool-id) :groups_role])
      (add-suspended-until-to-query inventory-pool-id)))

(defn users-formated-query [inventory-pool-id request]
  (-> (users-query inventory-pool-id request)
      sql-format))

;;; suspsensions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-query [inventory-pool-id user-id]
  (-> (sql/select :*)
      (sql/from :suspensions)
      (sql/where [:= :user_id user-id])
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:raw "CURRENT_DATE <= suspended_until"])))

(defn user-suspension [tx inventory-pool-id user-id]
  (->> (suspension-query inventory-pool-id user-id)
       sql-format
       (jdbc-query tx)
       first))

(defn user-add-suspension [tx inventory-pool-id user]
  (assoc user :suspension (user-suspension tx inventory-pool-id (:id  user))))

;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users [{{inventory-pool-id :inventory-pool-id} :route-params
              tx :tx :as request}]
  (let [inventory-pool-id (normalized-inventory-pool-id! inventory-pool-id tx)
        query (users-query inventory-pool-id request)
        offset (:offset query)]
    {:body
     {:users (-> query
                 sql-format
                 (->> (jdbc-query tx)
                      (map #(role-to-roles-map [:role] %))
                      (map #(role-to-roles-map [:direct_role] %))
                      (map #(role-to-roles-map [:groups_role] %))
                      (map #(rename-keys % {:role :roles
                                            :direct_role :direct_roles
                                            :groups_role :groups_roles}))
                      (map (fn [user] (update-in user [:suspension]
                                                 #(-> % first (select-keys [:suspended_until :suspended_reason])))))
                      (seq/with-index offset)
                      seq/with-page-index))}}))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
