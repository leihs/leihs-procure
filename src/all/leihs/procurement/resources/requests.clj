(ns leihs.procurement.resources.requests
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [leihs.procurement.utils.sql :as sql]
    [clojure.java.jdbc :as jdbc]))

(defn requests-base-query []
  (-> (sql/select :*)
      (sql/from :procurement_requests)))

(defn requests-query [context arguments]
  (let [category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        priority (:priority arguments)
        inspector-priority (:inspector_priority arguments)
        requested-by-auth-user (:requested_by_auth_user arguments)
        ; state (:state arguments)
        ]
    (debug/identity-with-logging
      (sql/format
        (cond-> (requests-base-query)
          category-id
          (sql/merge-where [:in :procurement_requests.category_id category-id])

          budget-period-id
          (sql/merge-where [:in :procurement_requests.budget_period_id budget-period-id])

          organization-id
          (sql/merge-where [:in :procurement_requests.organization_id organization-id])

          priority
          (sql/merge-where [:in :procurement_requests.priority priority])

          inspector-priority
          (sql/merge-where [:in :procurement_requests.inspector_priority inspector-priority])

          requested-by-auth-user
          (sql/merge-where [:=
                            :procurement_requests.user_id
                            (sql/call :cast (-> context :request :authenticated-entity :id) :uuid)]) 
          )))))

  (defn get-requests [context arguments]
    (jdbc/query (-> context :request :tx) (requests-query context arguments)))

  ;#### debug ###################################################################
  (logging-config/set-logger! :level :debug)
  ; (logging-config/set-logger! :level :info)
  ; (debug/debug-ns 'cider-ci.utils.shutdown)
  ; (debug/debug-ns *ns*)
  ; (debug/undebug-ns *ns*)
