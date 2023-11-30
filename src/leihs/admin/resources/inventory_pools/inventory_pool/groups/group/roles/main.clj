(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set :as set]
   [compojure.core :as cpj]
   [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy]]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.main :refer [group-roles]]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions :as lmr]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.regex :as regex]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn role-query [inventory-pool-id group-id]
  (-> (sql/select :role)
      (sql/from :group_access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :group_id group-id])))

(defn get-roles
  [{{inventory-pool-id :inventory-pool-id group-id :group-id} :route-params
    tx :tx :as request}]
  {:body (group-roles tx inventory-pool-id group-id)})

(defn set-roles
  [{{inventory-pool-id :inventory-pool-id group-id :group-id} :route-params
    tx :tx roles :body :as request}]
  (lmr/protect-inventory-manager-escalation-by-lending-manager! request)
  (lmr/protect-inventory-manager-restriction-by-lending-manager! role-query request)
  (if-let [allowed-role-key (some->> roles/allowed-states
                                     (into [])
                                     (filter #(= roles (second %)))
                                     first first)]
    (do (jdbc/delete! tx :group_access_rights ["inventory_pool_id = ? AND group_id =? " inventory-pool-id group-id])
        (when (not= allowed-role-key :none)
          (jdbc/insert! tx :group_access_rights {:inventory_pool_id inventory-pool-id
                                                 :group_id group-id
                                                 :role (str allowed-role-key)}))
        (get-roles request))
    {:status 422 :data {:message "Submitted combination of roles is not allowed!"}}))
;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-group-roles-path
  (path :inventory-pool-group-roles
        {:inventory-pool-id ":inventory-pool-id"
         :group-id ":group-id"}))

(def routes
  (cpj/routes
   (cpj/GET inventory-pool-group-roles-path [] #'get-roles)
   (cpj/PUT inventory-pool-group-roles-path [] #'set-roles)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
