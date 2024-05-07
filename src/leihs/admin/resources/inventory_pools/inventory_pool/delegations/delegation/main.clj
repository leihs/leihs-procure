(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries :as queries]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.responsible-user :as responsible-user]
   [logbug.debug :as debug]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [delete! insert! query update!] :rename {query jdbc-query,
                                                                  insert! jdbc-insert!,
                                                                  update! jdbc-update!,
                                                                  delete! jdbc-delete!}]
   [taoensso.timbre :as timbre :refer [debug info warn]]))

;;; data keys ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def responsible-user-email-subquery
  (-> (sql/select :responsible_users.email)
      (sql/from [:users :responsible_users])
      (sql/where [:= :responsible_users.id :delegations.delegator_user_id])
      (sql/where [:<> nil :responsible_users.email])))

(defn delegation-selects [inventory-pool-id]
  [:created_at
   [:firstname :name]
   :id
   :pool_protected
   :updated_at
   [[:case
     [:exists responsible-user-email-subquery] responsible-user-email-subquery
     :else [:cast :delegations.delegator_user_id :text]]
    :responsible_user_id]
   [queries/contracts-count :contracts_count]
   [(queries/contracts-count-per-pool inventory-pool-id) :contracts_count_per_pool]
   [(queries/contracts-count-open-per-pool inventory-pool-id) :contracts_count_open_per_pool]
   [queries/direct-users-count :direct_users_count]
   [queries/groups-count :groups_count]
   [queries/pools-count :pools_count]
   [queries/users-count :users_count]
   [queries/responsible-user :responsible_user]])

(def delegation-write-keys
  [:name
   :delegator_user_id
   :pool_protected])

(def delegation-write-keymap
  {:name :firstname
   :responsible_user_id :delegator_user_id})

(defn other-pools-query [delegation inventory-pool-id]
  (-> (sql/select :id :name)
      (sql/order-by :name)
      (sql/from :inventory_pools)
      (sql/where [:<> :inventory_pools.id inventory-pool-id])
      (sql/where
       [:exists (-> (sql/select 1)
                    (sql/from :access_rights)
                    (sql/where
                     [:= :access_rights.inventory_pool_id
                      :inventory_pools.id])
                    (sql/where
                     [:= :access_rights.user_id (:id delegation)]))])))

(defn assoc-other-pools [inventory-pool-id delegation tx]
  (assoc delegation :other_pools
         (->> (-> delegation
                  (other-pools-query inventory-pool-id)
                  sql-format)
              (jdbc-query tx))))

;;; delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn in-pool-subquery [inventory-pool-id delegation-id]
  (-> (sql/select 1)
      (sql/from :access_rights)
      (sql/where [:= :access_rights.inventory_pool_id inventory-pool-id])
      (sql/where [:= :access_rights.user_id delegation-id])))

(defn delegation-query [inventory-pool-id delegation-id]
  (-> (apply sql/select (delegation-selects inventory-pool-id))
      (sql/from [:users :delegations])
      (sql/where [:= :delegations.id delegation-id])
      (sql/where [:<> :delegations.delegator_user_id nil])
      (sql/where [:exists (in-pool-subquery inventory-pool-id delegation-id)])
      sql-format))

(defn delegation-for-pool
  [{tx :tx
    {delegation-id :delegation-id
     inventory-pool-id :inventory-pool-id} :route-params}]
  (if-let [delegation (->> delegation-id
                           (delegation-query inventory-pool-id)
                           (jdbc-query tx) first)]
    {:status 200 :body (assoc-other-pools inventory-pool-id delegation tx)}
    {:status 404}))

;;; delete delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegation! [tx delegation-id]
  (or (->> (-> (sql/select :*)
               (sql/from :users)
               (sql/where [:= :id delegation-id])
               (sql/where [:<> nil :delegator_user_id])
               sql-format)
           (jdbc-query tx) first)
      (throw (ex-info "Delegation not found" {:status 404}))))

(defn can-delete-tx? [delegation-id tx]
  (let [sp (.setSavepoint (:connectable tx))]
    (try (jdbc-delete! tx :users ["id = ?" delegation-id])
         (.rollback (:connectable tx) sp)
         true
         (catch Throwable _
           (.rollback (:connectable tx) sp)
           false))))

(defn delete-delegation [{tx :tx
                          {inventory-pool-id :inventory-pool-id
                           delegation-id :delegation-id} :route-params}]
  (if (= 1 (::jdbc/update-count
            (jdbc-delete! tx :direct_access_rights
                          ["user_id = ? AND inventory_pool_id = ?"
                           delegation-id inventory-pool-id])))
    (do (when-not (-> (sql/select 1)
                      (sql/from :access_rights)
                      (sql/where [:= :access_rights.user_id delegation-id])
                      sql-format
                      (->> (jdbc-query tx))
                      first)
          (when (can-delete-tx? delegation-id tx)
            (jdbc-delete! tx :users ["id = ?" delegation-id])))
        {:status 204})
    {:status 404 :body "Removing delegation failed without error."}))

;;; update delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-delegation
  ([{tx :tx {delegation-id :delegation-id} :route-params
     {protected :pool_protected name :name uid :responsible_user_id} :body}]
   (if-let [ruid (-> uid  (responsible-user/find-by-unique-property tx) :id)]
     (let [update-count (::jdbc/update-count
                         (jdbc-update! tx :users
                                       {:delegator_user_id ruid
                                        :firstname name
                                        :pool_protected protected}
                                       ["id = ?" delegation-id]))]
       (if (= update-count 1)
         {:status 204}
         {:status 422 :body "Nothing updated."}))
     (throw responsible-user/not-found-ex))))

;;; add delegation to pool ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-delegation
  ([{{inventory-pool-id :inventory-pool-id delegation-id :delegation-id} :route-params
     tx :tx :as request}]
   (let [delegation (delegation! tx delegation-id)]
     (if (:pool_protected delegation)
       (throw (ex-info "Delegation is protected " {:status 403}))
       (if-not (jdbc-insert! tx :direct_access_rights
                             {:inventory_pool_id inventory-pool-id
                              :user_id delegation-id
                              :role "customer"})
         (throw (ex-info "Error adding delegation to pool" {:status 511}))
         {:status 204})))))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delegation-path
  (path :inventory-pool-delegation
        {:inventory-pool-id ":inventory-pool-id"
         :delegation-id ":delegation-id"}))

(defn routes [request]
  (case (:request-method request)
    :get (delegation-for-pool request)
    :put (add-delegation request)
    :patch (patch-delegation request)
    :delete (delete-delegation request)))

;(path :inventory-pool-delegations {:inventory-pool-id ":inventory-pool-id"})

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)
;(debug/debug-ns *ns*)
