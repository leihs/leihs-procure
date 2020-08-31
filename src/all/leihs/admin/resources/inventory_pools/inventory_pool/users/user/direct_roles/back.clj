(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [expand-role-to-hierarchy allowed-roles-states roles-to-map]]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions :refer [protect-inventory-manager-escalation-by-lending-manager! protect-inventory-manager-restriction-by-lending-manager!]]
    [leihs.admin.resources.users.back :as users]
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

(defn direct-access-rights-query [inventory-pool-id user-id]
  (-> (sql/select :role)
      (sql/from :direct_access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :user_id user-id])))

(defn roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (let [direct-access-rights (->> (direct-access-rights-query inventory-pool-id user-id)
                           sql/format (jdbc/query tx) first)
        roles (-> direct-access-rights :role keyword
                  expand-role-to-hierarchy
                  roles-to-map)]
    {:body {:roles roles}}))




(defn set-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx {roles :roles} :body :as request}]
  (protect-inventory-manager-escalation-by-lending-manager! request)
  (protect-inventory-manager-restriction-by-lending-manager! direct-access-rights-query request)
  (if-let [allowed-role-key (some->> allowed-roles-states
                                     (into [])
                                     (filter #(= roles (second %)))
                                     first first)]
    (do (jdbc/delete! tx :direct_access_rights ["inventory_pool_id = ? AND user_id =? " inventory-pool-id user-id])
        (when (not= allowed-role-key :none)
          (jdbc/insert! tx :direct_access_rights {:inventory_pool_id inventory-pool-id
                                                  :user_id user-id
                                                  :role (str allowed-role-key)}))
        {:status 204})
    {:status 422 :data {:message "Submitted combination of roles is not allowed!"}}))

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-direct-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx body :body :as request}]
  (jdbc/delete! tx :direct_access_rights ["inventory_pool_id = ? AND user_id = ?" inventory-pool-id user-id])
  {:status 204})


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-direct-roles-path
  (path :inventory-pool-user-direct-roles {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/DELETE inventory-pool-user-direct-roles-path [] #'delete-direct-roles)
    (cpj/GET inventory-pool-user-direct-roles-path [] #'roles)
    (cpj/PUT inventory-pool-user-direct-roles-path [] #'set-roles)))


;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
