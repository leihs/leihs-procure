(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.back
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [expand-role-to-hierarchy allowed-roles-states roles-to-map]]
    ;[leihs.admin.resources.inventory-pools.inventory-pool.users.back :refer [user-roles]]
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

(defn access-rights-query [inventory-pool-id user-id]
  (-> (sql/select :role :origin_table)
      (sql/from :access_rights)
      (sql/merge-where [:= :inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :user_id user-id])))

(defn roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx :as request}]
  (let [access-rights (->> (access-rights-query inventory-pool-id user-id)
                           sql/format (jdbc/query tx) first)
        roles (-> access-rights :role keyword
                  expand-role-to-hierarchy
                  roles-to-map)]
    {:body {:roles roles
            :origin_table (:origin_table access-rights)}}))


(defn set-roles
  [{{inventory-pool-id :inventory-pool-id user-id :user-id} :route-params
    tx :tx data :body :as request}]
  (let [roles (:roles data)]
    (logging/debug 'roles roles)
    (if-let [allowed-role-key (some->> allowed-roles-states
                                       (into [])
                                       (filter #(= roles (second %)))
                                       first first)]
      (do (jdbc/delete! tx :access_rights ["inventory_pool_id = ? AND user_id =? " inventory-pool-id user-id])
          (when (not= allowed-role-key :none)
            (jdbc/insert! tx :access_rights {:inventory_pool_id inventory-pool-id
                                             :user_id user-id
                                             :role (str allowed-role-key)}))
          {:status 204})
      {:status 422 :data {:message "Submitted combination of roles is not allowed!"}})))
;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def inventory-pool-user-roles-path
  (path :inventory-pool-user-roles {:inventory-pool-id ":inventory-pool-id" :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/GET inventory-pool-user-roles-path [] #'roles)
    (cpj/PUT inventory-pool-user-roles-path [] #'set-roles)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-access-right)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/debug-ns *ns*)
