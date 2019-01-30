(ns leihs.admin.resources.system.system-admins.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [leihs.core.system-admin :refer [system-admin-sql-expr]]

    [leihs.admin.auth.back :as admin-auth]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.resources.system.system-admins.shared :as shared]

    [clojure.java.jdbc :as jdbc]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn system-admins-query [request]
  (-> request
      users/users-query
      (sql/merge-where system-admin-sql-expr)
      (sql/merge-select [(sql/call :case system-admin-sql-expr true :else false)
                         :is_system_admin])))


(def system-admin-direct-users-count-query
  (-> (sql/select :%count.*)
      (sql/from :users)
      (sql/merge-where system-admin-sql-expr)
      (sql/format)))

(defn users-formated-query [request]
  (-> (system-admins-query request)
      sql/format))

(defn users [{tx :tx :as request}]
    {:body
     {:system-admin_users_count (->> system-admin-direct-users-count-query
                              (jdbc/query tx)
                              first :count)
      :users (->> (users-formated-query request)
                  (jdbc/query tx))}})



(def routes
  (-> (cpj/routes
        (cpj/GET (path :system-admins) [] #'users))
      (admin-auth/wrap-authorize #{} {:scope_admin_read true
                                      :scope_admin_write true
                                      :scope_system_admin_read true
                                      :scope_system_admin_write true})))

;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'system-admins-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
