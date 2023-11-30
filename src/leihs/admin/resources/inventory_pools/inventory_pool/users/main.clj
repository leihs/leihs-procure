(ns leihs.admin.resources.inventory-pools.inventory-pool.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set :as set]
   [clojure.set :refer [rename-keys]]
   [compojure.core :as cpj]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.shared :refer [default-query-params]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.regex :as regex]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-effective-role [query inventory-pool-id role]
  (-> query
      (sql/merge-join [:access_rights :far]
                      [:and
                       [:= :far.user_id :users.id]
                       [:= :far.inventory_pool_id inventory-pool-id]])
      (sql/merge-where [:in :far.role
                        (map str (roles/expand-to-hierarchy-up-and-include role))])))

(defn filter-for-none-role [query inventory-pool-id]
  (-> query
      (sql/merge-where
       [:not [:exists (-> (sql/select :true)
                          (sql/from [:access_rights :far])
                          (sql/merge-where [:= :far.inventory_pool_id inventory-pool-id])
                          (sql/merge-where [:= :far.user_id :users.id]))]])))

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
  (sql/merge-join query :suspensions
                  [:and
                   [:= :suspensions.user_id :users.id]
                   [:= :suspensions.inventory_pool_id inventory-pool-id]]))

(defn merge-aggreaged-role-to-query [query inventory-pool-id]
  (-> query
      (sql/merge-left-join :access_rights
                           [:and
                            [:= :users.id :access_rights.user_id]
                            [:= :access_rights.inventory_pool_id inventory-pool-id]])
      (sql/merge-select [:access_rights.role :role])))

(defn merge-direct-role-to-query [query inventory-pool-id]
  (-> query
      (sql/merge-left-join :direct_access_rights
                           [:and
                            [:= :users.id :direct_access_rights.user_id]
                            [:= :direct_access_rights.inventory_pool_id inventory-pool-id]])
      (sql/merge-select [:direct_access_rights.role :direct_role])))

;;; group access_rights ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-role-query [inventory-pool-id]
  (-> (sql/select (sql/call :role_agg :group_access_rights.role))
      (sql/from :group_access_rights)
      (sql/merge-where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
      (sql/merge-join :groups [:= :groups.id :group_access_rights.group_id])
      (sql/merge-join :groups_users [:= :groups.id :groups_users.group_id])
      (sql/merge-where [:= :groups_users.user_id :users.id])
      (sql/group :groups_users.user_id)))

(defn role-to-roles-map [ks data]
  (update-in data ks #(-> % roles/expand-to-hierarchy roles/roles-to-map)))

;;; users query ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-suspended-until-to-query
  ([query inventory-pool-id]
   (add-suspended-until-to-query query inventory-pool-id :users))
  ([query inventory-pool-id table]
   (-> query
       (sql/merge-select [(-> (sql/select (sql/raw " json_agg(to_json(suspensions.*))"))
                              (sql/from :suspensions)
                              (sql/merge-where [:= :suspensions.user_id (sql/qualify table :id)])
                              (sql/merge-where [:= :suspensions.inventory_pool_id inventory-pool-id])) :suspension]))))

;;; suspension filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-subquery
  ([inventory-pool-id] (suspension-subquery inventory-pool-id :users))
  ([inventory-pool-id table]
   (-> (sql/select :true)
       (sql/from :suspensions)
       (sql/merge-where [:= :suspensions.user_id (sql/qualify table :id)])
       (sql/merge-where [:= :suspensions.inventory_pool_id inventory-pool-id])
       (sql/merge-where (sql/raw  "CURRENT_DATE <= suspensions.suspended_until")))))

(defn filter-suspended
  ([query inventory-pool-id request]
   (filter-suspended query inventory-pool-id request :users))
  ([query inventory-pool-id request table]
   (case (-> request :query-params :suspension)
     "suspended" (sql/merge-where
                  query
                  [:exists (suspension-subquery inventory-pool-id table)])
     "unsuspended" (sql/merge-where
                    query
                    [:not [:exists (suspension-subquery inventory-pool-id table)]])
     query)))

;;; users query ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-query [inventory-pool-id request]
  (-> request
      users/users-query
      (sql/merge-select [inventory-pool-id :inventory_pool_id])
      (filter-by-role inventory-pool-id request)
      (filter-suspended inventory-pool-id request)
      (merge-aggreaged-role-to-query inventory-pool-id)
      (merge-direct-role-to-query inventory-pool-id)
      (sql/merge-select [(groups-role-query inventory-pool-id) :groups_role])
      (add-suspended-until-to-query inventory-pool-id)))

(defn users-formated-query [inventory-pool-id request]
  (-> (users-query inventory-pool-id request)
      sql/format))

;;; suspsensions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-query [inventory-pool-id user-id]
  (-> (sql/select :*)
      (sql/from :suspensions)
      (sql/merge-where [:= :user_id user-id])
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where (sql/raw  "CURRENT_DATE <= suspended_until"))))

(defn user-suspension [tx inventory-pool-id user-id]
  (->> (suspension-query inventory-pool-id user-id)
       sql/format
       (jdbc/query tx)
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
                 sql/format
                 (->> (jdbc/query tx)
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

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-path
  (path :inventory-pool-user {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def inventory-pool-users-path
  (path :inventory-pool-users {:inventory-pool-id ":inventory-pool-id"}))

(def routes
  (cpj/routes
   (cpj/GET inventory-pool-users-path [] #'users)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
