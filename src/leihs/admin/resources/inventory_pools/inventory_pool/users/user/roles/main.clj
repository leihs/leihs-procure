(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.set :as set]
   [compojure.core :as cpj]
   [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy roles-to-map]]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.regex :as regex]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.sql :as sql]
   [logbug.debug :as debug]))

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn access-rights-query [inventory-pool-id user-id]
  (-> (sql/select :role :origin_table)
      (sql/from :access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :user_id user-id])))

(defn get-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (let [access-rights (->> (access-rights-query inventory-pool-id user-id)
                           sql/format (jdbc/query tx) first)
        roles (-> access-rights :role keyword
                  expand-to-hierarchy
                  roles-to-map)]
    {:body roles}))

(defn set-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx roles :body :as request}]
  (if-let [allowed-role-key (some->> roles/allowed-states
                                     (into [])
                                     (filter #(= roles (second %)))
                                     first first)]
    (do (jdbc/delete! tx :access_rights ["inventory_pool_id = ? AND user_id =? " inventory-pool-id user-id])
        (when (not= allowed-role-key :none)
          (jdbc/insert! tx :access_rights {:inventory_pool_id inventory-pool-id
                                           :user_id user-id
                                           :role (str allowed-role-key)}))
        (get-roles request))
    {:status 422 :data {:message "Submitted combination of roles is not allowed!"}}))

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-roles-path
  (path :inventory-pool-user-roles {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
   (cpj/GET inventory-pool-user-roles-path [] #'get-roles)
   (cpj/PUT inventory-pool-user-roles-path [] #'set-roles)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
