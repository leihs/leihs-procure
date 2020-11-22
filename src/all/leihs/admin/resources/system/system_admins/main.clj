(ns leihs.admin.resources.system.system-admins.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]
    [leihs.core.system-admin :refer [system-admin-sql-expr]]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.resources.system.system-admins.shared :as shared]
    [leihs.admin.utils.jdbc :as utils.jdbc]

    [clojure.java.jdbc :as jdbc]
    [clojure.set :as set]
    [clojure.set]
    [compojure.core :as cpj]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


;;; get-users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-by-is-system-admin
  [query {{is-system-admin :is-system-admin} :query-params :as request}]
  (case (or (presence is-system-admin)
            (:is-system-admin shared/default-query-params))
    "any" query
    "yes" (sql/merge-where query system-admin-sql-expr)
    "no"  (sql/merge-where query [:not system-admin-sql-expr])))

(defn system-admins-query [request]
  (-> request
      users/users-query
      (filter-by-is-system-admin request)
      (sql/merge-select [(sql/call :case system-admin-sql-expr true
                                   :else false)
                         :is_system_admin])))

(defn users-formated-query [request]
  (-> request
      system-admins-query
      sql/format))

(defn get-users [{tx :tx :as request}]
  {:body
   {:users (->> request users-formated-query
                (jdbc/query tx))}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-user [{tx :tx :as request
                 body :body
                 {user-id :user-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :system_admin_users
    ["user_id = ?" user-id]
    {:user_id user-id})
  {:status 204})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {user-id :user-id} :route-params}]
  (if (= 1 (->> ["user_id = ?" user-id]
                (jdbc/delete! tx :system_admin_users)
                first))
    {:status 204}
    (throw (ex-info "Remove system_admin_users failed" {:status 409}))))


;;; paths and routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system-admin-path (path :system-admin {:user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/GET (path :system-admins) [] #'get-users)
    (cpj/PUT system-admin-path [] #'add-user)
    (cpj/DELETE system-admin-path [] #'remove-user)))


;#### debug ###################################################################
;(debug/debug-ns *ns*)
;(debug/wrap-with-log-debug #'system-admins-formated-query)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
