(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [leihs.core.sql :as sql]

    [leihs.admin.paths :refer [path]]

    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries :as queries]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.responsible-user :as responsible-user]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared :refer [default-query-params]]
    [leihs.admin.resources.users.main :as users]

    [clojure.java.jdbc :as jdbc]
    [compojure.core :as cpj]

    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    ))


(def delegations-base-query
  (-> (sql/select :delegations.id
                  :delegations.firstname
                  :delegations.protected)
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

(defn set-per-page-and-offset
  ([query {{per-page :per-page page :page} :query-params}]
   (when (or (-> per-page presence not)
             (-> per-page integer? not)
             (> per-page 1000)
             (< per-page 1))
     (throw (ex-info "The query parameter per-page must be present and set to an integer between 1 and 1000."
                     {:status 422})))
   (when (or (-> page presence not)
             (-> page integer? not)
             (< page 0))
     (throw (ex-info "The query parameter page must be present and set to a positive integer."
                     {:status 422})))
   (set-per-page-and-offset query per-page page))
  ([query per-page page]
   (-> query
       (sql/limit per-page)
       (sql/offset (* per-page (- page 1))))))

(defn term-fitler [query request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (-> query
        (sql/merge-where [:or
                          ["%" (str term) :searchable]
                          ["~~*" :searchable (str "%" term "%")]]))
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
             [:= :delegations.protected :false]])
    "non" (-> query
              (sql/merge-where [:and [:not (queries/member-expr inventory-pool-id)]
                                [:= :delegations.protected :false]]))
    "member" (sql/merge-where query (queries/member-expr inventory-pool-id))))

(defn delegations-query
  [{{inventory-pool-id :inventory-pool-id} :route-params
    :as request}]
  (-> delegations-base-query
      (merge-select-counts inventory-pool-id)
      (inventory-pool-filter request)
      (set-per-page-and-offset request)
      (term-fitler request)
      sql/format))

(defn delegations
  [{:as request tx :tx
    {inventory-pool-id :inventory-pool-id } :route-params }]
  {:body
   {:delegations
    (jdbc/query (:tx request) (delegations-query request))}})


;;; create delegation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-delegation
  [{tx :tx
    {inventory-pool-id :inventory-pool-id} :route-params
    {protected :protected name :name uid :responsible_user_id} :body}]
  (if-let [ruid (-> uid (responsible-user/find-by-unique-property tx) :id)]
    (if-let [delegation (first (jdbc/insert! tx :users
                                             {:delegator_user_id ruid
                                              :firstname name
                                              :protected protected}))]
      (if (first (jdbc/insert! tx :delegations_users
                               {:delegation_id (:id delegation)
                                :user_id ruid}))
        (if (first (jdbc/insert! tx :direct_access_rights
                                 {:user_id (:id delegation)
                                  :inventory_pool_id inventory-pool-id
                                  :role "customer" }))
          {:status 200 :body delegation}
          (throw (ex-info
                   "failed to add delegation as customer to pool" {:status 422})))
        (throw (ex-info "the responsible user could not be added as a delegation member"
                        {:status 422})))
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
                                 :get  (delegations request)
                                 :post (create-delegation request))))


;#### debug ###################################################################
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
(debug/debug-ns *ns*)
