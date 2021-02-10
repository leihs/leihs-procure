(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.common.membership.users.main :refer [extend-with-membership]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.jdbc :as utils.jdbc]
    [leihs.admin.utils.seq :as seq]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]))


(defn member-expr [delegation-id]
  [:exists
   (-> (sql/select true)
       (sql/from :delegations_users)
       (sql/merge-where [:= :users.id :delegations_users.user_id])
       (sql/merge-where [:= :delegations_users.delegation_id delegation-id]))])


(defn direct-member-expr [delegation-id]
  [:exists
   (-> (sql/select true)
       (sql/from :delegations_direct_users)
       (sql/merge-where [:= :users.id :delegations_direct_users.user_id])
       (sql/merge-where [:= :delegations_direct_users.delegation_id delegation-id]))])

(defn group-member-expr [delegation-id]
  [:exists
   (-> (sql/select true)
       (sql/from :delegations_groups)
       (sql/merge-where [:= :delegations_groups.delegation_id delegation-id])
       (sql/merge-join :groups [:= :delegations_groups.group_id :groups.id])
       (sql/merge-join :groups_users [:= :groups_users.group_id :groups.id])
       (sql/merge-where [:= :users.id :groups_users.user_id]))])

(defn users-query [{{delegation-id :delegation-id} :route-params
                    :as request}]
  (-> (users/users-query request)
      (extend-with-membership
        (member-expr delegation-id)
        (direct-member-expr delegation-id)
        (group-member-expr delegation-id)
        request)))

(defn users-formated-query [request]
  (-> request
      users-query
      sql/format))

(defn users [{tx :tx :as request}]
  (let [query (users-query request)
        offset (:offset query)]
    {:body
     {:users (-> query sql/format
                 (->> (jdbc/query tx)
                      (seq/with-index offset)
                      seq/with-page-index))}}))

;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-user [{tx :tx :as request
                 {delegation-id :delegation-id
                  user-id :user-id} :route-params}]
  (utils.jdbc/insert-or-update!
    tx :delegations_direct_users ["delegation_id = ? AND user_id = ?" delegation-id user-id]
    {:delegation_id delegation-id :user_id user-id})
  {:status 204})


;;; remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [{tx :tx :as request
                    {delegation-id :delegation-id
                     user-id :user-id} :route-params}]
  (if (= 1 (->> ["delegation_id = ? AND user_id = ?" delegation-id user-id]
                (jdbc/delete! tx :delegations_direct_users)
                first))
    {:status 204}
    (throw (ex-info "Remove delegation-user failed" {:request request}))))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delegation-user-path
  (path :inventory-pool-delegation-user
        {:inventory-pool-id ":inventory-pool-id"
         :delegation-id ":delegation-id"
         :user-id ":user-id"}))

(def delegation-users-path
  (path :inventory-pool-delegation-users
        {:inventory-pool-id ":inventory-pool-id"
         :delegation-id ":delegation-id"}))

(def routes
  (cpj/routes
    (cpj/PUT delegation-user-path [] #'put-user)
    (cpj/DELETE delegation-user-path [] #'remove-user)
    (cpj/GET delegation-users-path [] #'users)))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
