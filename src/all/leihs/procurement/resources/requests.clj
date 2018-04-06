(ns leihs.procurement.resources.requests
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]))

(def state-sql
  (sql/call :case
            [:= :procurement_requests.approved_quantity nil]
            "new"
            [:= :procurement_requests.approved_quantity 0]
            "denied"
            [:and
             [:< 0 :procurement_requests.approved_quantity]
             [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity]]
            "partially_approved"
            [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity]
            "approved"))

(def requests-base-query
  (-> (sql/select :procurement_requests.* [state-sql :state])
      (sql/from :procurement_requests)))

(defn requests-query [context arguments _]
  (let [category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        priority (:priority arguments)
        inspector-priority (:inspector_priority arguments)
        requested-by-auth-user (:requested_by_auth_user arguments)
        from-categories-of-auth-user (:from_categories_of_auth_user arguments)
        state (:state arguments)]
    (sql/format
      (cond-> requests-base-query
        (not (empty? category-id))
        (sql/merge-where
          [:in :procurement_requests.category_id category-id])

        (not (empty? budget-period-id))
        (sql/merge-where
          [:in :procurement_requests.budget_period_id budget-period-id])

        (not (empty? organization-id))
        (sql/merge-where
          [:in :procurement_requests.organization_id organization-id])

        (not (empty? priority))
        (sql/merge-where
          [:in :procurement_requests.priority priority])

        (not (empty? inspector-priority))
        (sql/merge-where
          [:in :procurement_requests.inspector_priority inspector-priority])

        (not (empty? state))
        (sql/merge-where [:in state-sql state])

        requested-by-auth-user
        (sql/merge-where
          [:=
           :procurement_requests.user_id
           (-> context :request :authenticated-entity :id)]) 

        from-categories-of-auth-user
        (sql/merge-where
          [:in
           :procurement_requests.category_id
           (-> (sql/select :category_id)
               (sql/from :procurement_category_inspectors)
               (sql/merge-where [:=
                                 :procurement_category_inspectors.user_id
                                 (-> context :request :authenticated-entity :id)]))])
        ))))

(defn get-requests [context arguments value]
  (if (some #(= (% arguments) [])
            [:budget_period_id :category_id :organization_id :user_id])
    []
    (jdbc/query (-> context :request :tx)
                (requests-query context arguments value))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
