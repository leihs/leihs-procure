(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]
    [leihs.core.routing.back :as routing :refer [set-per-page-and-offset mixin-default-query-params]]

    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries :as queries]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.responsible-user :as responsible-user]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared :refer [default-query-params]]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :refer [filter-suspended add-suspended-until-to-query]]
    [leihs.admin.resources.users.choose-core :as choose-user]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.seq :as seq]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]


    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def delegations-base-query
  (-> (sql/select :delegations.id
                  :delegations.firstname
                  :delegations.pool_protected)
      (sql/from [:users :delegations])
      (sql/order-by :delegations.firstname)
      (sql/merge-where [:<> nil :delegations.delegator_user_id])))


(defn merge-select-counts [query inventory-pool-id]
  (-> query
      (sql/merge-select [(sql/call :case
                                   (queries/member-expr inventory-pool-id) true
                                   :else false) :member])
      (sql/merge-select [queries/contracts-count :contracts_count])
      (sql/merge-select [queries/direct-users-count :direct_users_count])
      (sql/merge-select [queries/groups-count :groups_count])
      (sql/merge-select [queries/pools-count :pools_count])
      (sql/merge-select [queries/users-count :users_count])
      (sql/merge-select [queries/responsible-user :responsible_user])
      (sql/merge-select [(queries/contracts-count-per-pool inventory-pool-id) :contracts_count_per_pool])
      (sql/merge-select [(queries/contracts-count-open-per-pool inventory-pool-id) :contracts_count_open_per_pool])))

(defn term-fitler [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))
    query))

(defn filter-for-including-user
  [query {{user-uid :including-user} :query-params-raw :as request}]
  (if-let [user-uid (presence user-uid)]
    (sql/merge-where
      query
      [:exists
       (-> (choose-user/find-by-some-uid-query user-uid)
           (sql/select :true)
           (sql/merge-join :delegations_users [:= :delegations_users.delegation_id :delegations.id])
           (sql/merge-where [:= :delegations_users.user_id :users.id]))])
    query))

(defn inventory-pool-filter
  [query {{inventory-pool-id :inventory-pool-id} :route-params
          query-params :query-params
          :as request}]
  (case (-> (merge default-query-params query-params) :membership)
    "any" (sql/merge-where
            query
            [:or
             (queries/member-expr inventory-pool-id)
             [:= :delegations.pool_protected :false]])
    "non" (-> query
              (sql/merge-where [:and [:not (queries/member-expr inventory-pool-id)]
                                [:= :delegations.pool_protected :false]]))
    "member" (sql/merge-where query (queries/member-expr inventory-pool-id))))

(defn delegations-query
  [{{inventory-pool-id :inventory-pool-id} :route-params
    :as request}]
  (-> delegations-base-query
      (filter-for-including-user request)
      (merge-select-counts inventory-pool-id)
      (inventory-pool-filter request)
      (filter-suspended inventory-pool-id request :delegations)
      (add-suspended-until-to-query inventory-pool-id :delegations)
      (set-per-page-and-offset request)
      (term-fitler request)))

(defn delegations
  [{:as request tx :tx}]
  (let [query (delegations-query request)
        offset (:offset query)]
    {:body
     {:delegations (-> query sql/format
                       (->> (jdbc/query tx)
                            (map (fn [del]
                                   (update-in del [:suspension]
                                              #(-> %
                                                   first
                                                   (select-keys [:suspended_until :suspended_reason])))))

                            (seq/with-index offset)
                            seq/with-page-index))}}))


;;; create delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-delegation
  [{tx :tx
    {inventory-pool-id :inventory-pool-id} :route-params
    {protected :pool_protected name :name uid :responsible_user_id} :body}]
  (if-let [ruid (-> uid (responsible-user/find-by-unique-property tx) :id)]
    (if-let [delegation (first (jdbc/insert! tx :users
                                             {:delegator_user_id ruid
                                              :firstname name
                                              :pool_protected protected}))]
      (if (first (jdbc/insert! tx :direct_access_rights
                               {:user_id (:id delegation)
                                :inventory_pool_id inventory-pool-id
                                :role "customer" }))
        {:status 200 :body delegation}
        (throw (ex-info
                 "failed to add delegation as customer to pool" {:status 422})))
      (throw (ex-info  "The delegation could not be created!" {:status 422})))
    (throw responsible-user/not-found-ex)))


;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delegations-path
  (path :inventory-pool-delegations
        {:inventory-pool-id ":inventory-pool-id"} {}))


(defn routes [{request-method :request-method
               handler-key :handler-key
               :as request}]
  (case handler-key
    :inventory-pool-delegations (case request-method
                                 :get  (-> request
                                           (mixin-default-query-params default-query-params)
                                           delegations)
                                 :post (create-delegation request))))


;#### debug ###################################################################


;(debug/debug-ns *ns*)
