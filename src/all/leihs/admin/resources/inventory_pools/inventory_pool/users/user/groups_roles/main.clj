(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [expand-role-to-hierarchy allowed-roles-states roles-to-map]]
    ; [leihs.admin.resources.inventory-pools.inventory-pool.users.main :refer [user-roles]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.sql :as sql]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))

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
                                                 expand-role-to-hierarchy roles-to-map)))))]
    {:body {:groups-roles groups-roles}}))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-groups-roles-path
  (path :inventory-pool-user-groups-roles {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-user-groups-roles-path [] #'groups-roles)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
