(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.shared :refer [default-query-params]]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword presence str]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [spy]]))

;;; groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-effective-role [query inventory-pool-id role]
  (-> query
      (sql/join :group_access_rights
                [:and
                 [:= :group_access_rights.group_id :groups.id]
                 [:= :group_access_rights.inventory_pool_id inventory-pool-id]])
      (sql/where [:in :group_access_rights.role
                  (map name (roles/expand-to-hierarchy-up-and-include role))])))

(defn filter-for-none-role [query inventory-pool-id]
  (-> query
      (sql/where
       [:not [:exists (-> (sql/select :true)
                          (sql/from :group_access_rights)
                          (sql/where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
                          (sql/where [:= :group_access_rights.group_id :groups.id]))]])))

(defn filter-by-role [query inventory-pool-id {:as request}]
  (let [role (-> (merge default-query-params
                        (:query-params request))
                 :role presence ((fnil name "")))]
    (case role
      "" query
      "none" (filter-for-none-role query inventory-pool-id)
      ("customer"
       "group_manager"
       "lending_manager"
       "inventory_manager") (filter-effective-role
                             query inventory-pool-id role))))

(defn groups-query [inventory-pool-id request]
  (-> request groups/groups-query
      (sql/select [inventory-pool-id :inventory_pool_id])
      (filter-by-role inventory-pool-id request)))

(defn role-query [inventory-pool-id group-id]
  (-> (sql/select :role)
      (sql/from :group_access_rights)
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :group_id group-id])))

(defn group-roles [tx inventory-pool-id group-id]
  (let [role-kw (->> (role-query inventory-pool-id group-id)
                     sql-format
                     (jdbc-query tx)
                     first :role keyword)]
    (-> role-kw
        roles/expand-to-hierarchy
        roles/roles-to-map)))

(defn group-add-roles [tx inventory-pool-id group]
  (assoc group :roles (group-roles tx inventory-pool-id (:id  group))))

(defn groups [{{inventory-pool-id :inventory-pool-id} :route-params
               tx :tx :as request}]
  (let [inventory-pool-id (normalized-inventory-pool-id! inventory-pool-id tx)
        query (groups-query inventory-pool-id request)
        offset (:offset query)]
    {:body
     {:groups (-> query (sql-format {:inline true})
                  spy
                  (->>
                   (jdbc-query tx)
                   (map (fn [group] (group-add-roles tx inventory-pool-id group)))
                   (seq/with-index offset)
                   seq/with-page-index doall))}}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case :get (groups request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-suspended)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
