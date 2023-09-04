(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.set :as set]
    [compojure.core :as cpj]
    [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy roles-to-map]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.sql :as sql]
    [logbug.debug :as debug]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-roles-query [inventory-pool-id user-id]
  (-> (sql/select [:group_access_rights.role :roles]
                  [:groups.name :group_name]
                  [:groups.id :group_id])
      (sql/from :group_access_rights)
      (sql/merge-join :groups [:= :groups.id :group_access_rights.group_id])
      (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/merge-where [:= :group_access_rights.inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :groups_users.user_id user-id])))

(defn groups-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (let [groups-roles (->> (groups-roles-query inventory-pool-id user-id)
                          sql/format (jdbc/query tx)
                          (map #(update-in % [:roles]
                                           (fn [role]
                                             (-> role keyword
                                                 expand-to-hierarchy roles-to-map))))
                          seq/with-page-index)]
    {:body {:groups-roles groups-roles}}))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-groups-roles-path
  (path :inventory-pool-user-groups-roles {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-user-groups-roles-path [] #'groups-roles)))


;#### debug ###################################################################


;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
