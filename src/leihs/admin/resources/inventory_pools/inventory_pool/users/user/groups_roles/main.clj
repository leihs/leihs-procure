(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy
                                                    roles-to-map]]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.core :refer [keyword]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-roles-query [inventory-pool-id user-id]
  (-> (sql/select [:group_access_rights.role :roles]
                  [:groups.name :group_name]
                  [:groups.id :group_id])
      (sql/from :group_access_rights)
      (sql/join :groups [:= :groups.id :group_access_rights.group_id])
      (sql/join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
      (sql/where [:= :groups_users.user_id user-id])))

(defn groups-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (let [groups-roles (->> (groups-roles-query inventory-pool-id user-id)
                          sql-format (jdbc-query tx)
                          (map #(update-in % [:roles]
                                           (fn [role]
                                             (-> role keyword
                                                 expand-to-hierarchy roles-to-map))))
                          seq/with-page-index)]
    {:body {:groups-roles groups-roles}}))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
