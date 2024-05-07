(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.main :refer [group-roles]]
   [leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions :as lmr]
   [leihs.core.core :refer [str]]
   [next.jdbc.sql :refer [insert! delete!] :rename {insert! jdbc-insert!, delete! jdbc-delete!}]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn role-query [inventory-pool-id group-id]
  (-> (sql/select :role)
      (sql/from :group_access_rights)
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :group_id group-id])))

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
    (do (jdbc-delete! tx :group_access_rights ["inventory_pool_id = ? AND group_id =? " inventory-pool-id group-id])
        (when (not= allowed-role-key :none)
          (jdbc-insert! tx :group_access_rights {:inventory_pool_id inventory-pool-id
                                                 :group_id group-id
                                                 :role (name allowed-role-key)}))
        (-> (get-roles request) (assoc :status 200)))
    {:status 422 :data {:message "Submitted combination of roles is not allowed!"}}))
;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-roles request)
    :put (set-roles request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'groups-formated-query)
;(debug/debug-ns *ns*)
