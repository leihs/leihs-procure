(ns leihs.admin.resources.inventory-pools.inventory-pool.shared-lending-manager-restrictions
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.resources.inventory-pools.authorization :refer [pool-access-right-for-route]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [spy]]))

(defn acts-as-lending-manger? [{authenticated-entity :authenticated-entity :as request}]
  (boolean (and (not (:scope_admin_write authenticated-entity))
                (= "lending_manager" (-> request pool-access-right-for-route :role)))))

(defn assert-roles-structure! [roles]
  (when-not (= (set roles/hierarchy) (-> roles keys set))
    (throw (ex-info "Roles data does not conform to expected structure"
                    {:status 422}))))

(defn protect-inventory-manager-escalation-by-lending-manager!
  [{roles :body :as request}]
  (assert-roles-structure! roles)
  (when (and (:inventory_manager roles)
             (acts-as-lending-manger? request))
    (throw
     (ex-info
      "A lending_manager may not escalate roles to an inventory_manager"
      {:status 403}))))

(defn protect-inventory-manager-restriction-by-lending-manager!
  [access-rights-query
   {{inventory-pool-id :inventory-pool-id user-id :user-id group-id :group-id} :route-params
    tx :tx-next roles :body :as request}]
  (assert-roles-structure! roles)
  (when (and (not (:inventory_manager roles))
             (acts-as-lending-manger? request))
    (when-let [existing-access-right (->> (access-rights-query
                                           inventory-pool-id (or group-id  user-id))
                                          sql-format (jdbc-query tx) first)]
      (when (= (:role existing-access-right) "inventory_manager")
        (throw
         (ex-info
          "A lending_manager may not restrict the roles of an inventory_manager"
          {:status 403}))))))

;#### debug ###################################################################
;(debug/debug-ns *ns*)

;(debug/wrap-with-log-debug #'filter-by-access-right)
