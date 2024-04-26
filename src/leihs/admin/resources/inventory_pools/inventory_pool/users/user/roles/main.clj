(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy
                                                    roles-to-map]]
   [leihs.core.core :refer [keyword str]]
   [next.jdbc.sql :refer [delete! insert! query] :rename {query jdbc-query,
                                                          delete! jdbc-delete!,
                                                          insert! jdbc-insert!}]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn access-rights-query [inventory-pool-id user-id]
  (-> (sql/select :role :origin_table)
      (sql/from :access_rights)
      (sql/where [:= :inventory_pool_id inventory-pool-id])
      (sql/where [:= :user_id user-id])))

(defn get-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx-next :as request}]
  (let [access-rights (->> (access-rights-query inventory-pool-id user-id)
                           sql-format (jdbc-query tx) first)
        roles (-> access-rights :role keyword
                  expand-to-hierarchy
                  roles-to-map)]
    {:body roles}))

(defn set-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx-next roles :body :as request}]
  (if-let [allowed-role-key (some->> roles/allowed-states
                                     (into [])
                                     (filter #(= roles (second %)))
                                     first first)]
    (do (jdbc-delete! tx :access_rights ["inventory_pool_id = ? AND user_id =? " inventory-pool-id user-id])
        (when (not= allowed-role-key :none)
          (jdbc-insert! tx :access_rights {:inventory_pool_id inventory-pool-id
                                           :user_id user-id
                                           :role (name allowed-role-key)}))
        (-> (get-roles request) (assoc :status 200)))
    {:status 422 :data {:message "Submitted combination of roles is not allowed!"}}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  (fn [request]
    (case (:request-method request)
      :get (get-roles request)
      :put (set-roles request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
