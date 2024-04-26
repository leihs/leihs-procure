(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [bidi.bidi :refer [match-route]]
   [clojure.core.match :refer [match]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.common.membership.users.main :refer [extend-with-membership]]
   [leihs.admin.paths :refer [paths]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.jdbc :as utils.jdbc]
   [leihs.admin.utils.seq :as seq]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! query] :rename {query jdbc-query, delete! jdbc-delete!}]))

;;; users ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_users)
       (sql/where [:= :users.id :entitlement_groups_users.user_id])
       (sql/where [:= :entitlement_groups_users.entitlement_group_id entitlement-group-id]))])

(defn direct-member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_direct_users)
       (sql/where [:= :users.id :entitlement_groups_direct_users.user_id])
       (sql/where [:= :entitlement_groups_direct_users.entitlement_group_id entitlement-group-id]))])

(defn group-member-expr [entitlement-group-id]
  [:exists
   (-> (sql/select true)
       (sql/from :entitlement_groups_groups)
       (sql/where [:= :entitlement_groups_groups.entitlement_group_id entitlement-group-id])
       (sql/join :groups [:= :entitlement_groups_groups.group_id :groups.id])
       (sql/join :groups_users [:= :groups_users.group_id :groups.id])
       (sql/where [:= :users.id :groups_users.user_id]))])

(defn users-query
  [{{entitlement-group-id :entitlement-group-id} :route-params
    :as request}]
  (-> (users/users-query request)
      (extend-with-membership
       (member-expr entitlement-group-id)
       (direct-member-expr entitlement-group-id)
       (group-member-expr entitlement-group-id)
       request)))

(defn users [{tx :tx-next :as request}]
  (let [query (-> request users-query)
        offset (:offset query)]
    {:body
     {:users (-> query sql-format
                 (->> (jdbc-query tx)
                      (seq/with-index offset)
                      seq/with-page-index))}}))

;;; remove direct user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-direct-user
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     user-id :user-id} :route-params
    tx :tx-next :as request}]
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :entitlement_groups_direct_users
                          ["entitlement_group_id = ? AND user_id = ?"
                           entitlement-group-id user-id])))
    {:status 204}
    {:status 404 :body "Remove direct entitlement user failed without error."}))

;;; add direct user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-direct-user
  [{{inventory-pool-id :inventory-pool-id
     entitlement-group-id :entitlement-group-id
     user-id :user-id} :route-params
    tx :tx-next :as request}]
  (utils.jdbc/insert-or-update!
   tx :entitlement_groups_direct_users
   ["entitlement_group_id = ? AND user_id = ?  " entitlement-group-id user-id]
   {:entitlement_group_id entitlement-group-id :user_id user-id})
  {:status 204})

;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (let [handler-key (->> request :uri (match-route paths) :handler)]
    (match [(:request-method request) handler-key]
      [:get :inventory-pool-entitlement-group-users] (users request)
      [:delete :inventory-pool-entitlement-group-direct-user] (remove-direct-user request)
      [:put :inventory-pool-entitlement-group-direct-user] (add-direct-user request))))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'filter-by-membership)
;(debug/wrap-with-log-debug #'users-formated-query)
;(debug/wrap-with-log-debug #'users-query)
;(debug/wrap-with-log-debug #'remove-direct-user)
;(debug/debug-ns *ns*)
