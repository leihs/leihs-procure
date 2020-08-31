(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.back
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
    [leihs.admin.resources.inventory-pools.inventory-pool.shared :refer [normalized-inventory-pool-id!]]
    [leihs.admin.resources.users.back :as users]
    [leihs.admin.shared.membership.users.back :refer [extend-with-membership]]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.regex :as regex]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]
    [clojure.set :as set]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_users)
       (sql/merge-where [:= :users.id :entitlement_groups_users.user_id])
       (sql/merge-where [:= :entitlement_groups_users.entitlement_group_id entitlement-group-id]))])

(defn direct-member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_direct_users)
       (sql/merge-where [:= :users.id :entitlement_groups_direct_users.user_id])
       (sql/merge-where [:= :entitlement_groups_direct_users.entitlement_group_id entitlement-group-id]))])

(defn group-member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_groups)
       (sql/merge-where [:= :entitlement_groups_groups.entitlement_group_id entitlement-group-id])
       (sql/merge-join :groups [:= :entitlement_groups_groups.group_id :groups.id])
       (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
       (sql/merge-where [:= :users.id :groups_users.user_id]))])

(defn users-query
  [{{entitlement-group-id :entitlement-group-id} :route-params
    :as request}]
  (-> (users/users-query request)
      (extend-with-membership
        (member-expr entitlement-group-id)
        (direct-member-expr entitlement-group-id)
        (group-member-expr entitlement-group-id)
        request)))

(defn users-formated-query [entitlement-group-id request]
  (-> (users-query request)
      sql/format))

(defn users [{{entitlement-group-id :entitlement-group-id} :route-params
              tx :tx :as request}]
  {:body
   {:users (->> (users-formated-query entitlement-group-id request)
                (jdbc/query tx)
                doall)}})

;;; remove direct user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-direct-user
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     user-id :user-id} :route-params
    tx :tx :as request }]
  (if (= [1] (jdbc/delete! tx :entitlement_groups_direct_users
                           ["entitlement_group_id = ? AND user_id = ?
                            " entitlement-group-id user-id]))
    {:status 204}
    {:status 404 :body "Remove direct entitlement user failed without error."}))



;;; add direct user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-direct-user
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     user-id :user-id} :route-params
    tx :tx :as request}]
  (utils.jdbc/insert-or-update!
    tx :entitlement_groups_direct_users
    ["entitlement_group_id = ? AND user_id = ?  " entitlement-group-id user-id]
    {:entitlement_group_id entitlement-group-id :user_id user-id})
  {:status 204})


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def users-path
  (path :inventory-pool-entitlement-group-users
        {:inventory-pool-id ":inventory-pool-id"
         :entitlement-group-id ":entitlement-group-id"}))

(def direct-user-path
  (path :inventory-pool-entitlement-group-direct-user
            {:inventory-pool-id ":inventory-pool-id"
             :entitlement-group-id ":entitlement-group-id"
             :user-id ":user-id"}))

(def routes
  (cpj/routes
    (cpj/GET users-path  [] #'users)
    (cpj/DELETE direct-user-path [] #'remove-direct-user)
    (cpj/PUT direct-user-path [] #'add-direct-user)))



;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/wrap-with-log-debug #'filter-by-membership)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-query)
;(debug/wrap-with-log-debug #'remove-direct-user)
;(debug/debug-ns *ns*)
