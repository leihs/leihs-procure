(ns leihs.procurement.resources.requests
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [leihs.procurement.resources.request :as request]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]))

(def requests-base-query
  (-> (sql/select :procurement_requests.* [request/state-sql :state])
      (sql/from :procurement_requests)))

(defn search-query [sql-query term]
  (let [term-percent (str "%" term "%")]
    (-> sql-query
        (sql/merge-join :rooms [:= :procurement_requests.room_id :rooms.id])
        (sql/merge-join :buildings [:= :rooms.building_id :buildings.id])
        (sql/merge-join :users [:= :procurement_requests.user_id :users.id])
        (sql/merge-where [:or
                          ["~~*" :buildings.name term-percent]
                          ["~~*" :procurement_requests.article_name term-percent]
                          ["~~*" :procurement_requests.article_number term-percent]
                          ["~~*" :procurement_requests.inspection_comment term-percent]
                          ["~~*" :procurement_requests.motivation term-percent]
                          ["~~*" :procurement_requests.receiver term-percent]
                          ["~~*" :procurement_requests.supplier_name term-percent]
                          ["~~*" :rooms.name term-percent]
                          ["~~*" :users.firstname term-percent]
                          ["~~*" :users.lastname term-percent]
                          ]))))

(defn requests-query [context arguments _]
  (let [category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        priority (:priority arguments)
        inspector-priority (:inspector_priority arguments)
        requested-by-auth-user (:requested_by_auth_user arguments)
        from-categories-of-auth-user (:from_categories_of_auth_user arguments)
        state (:state arguments)
        search-term (:search arguments)]
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
        (sql/merge-where [:in request/state-sql state])

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

        search-term
        (search-query search-term)
        ))))

(defn get-requests [context arguments value]
  (if (some #(= (% arguments) [])
            [:budget_period_id :category_id :organization_id :user_id])
    []
    (jdbc/query (-> context :request :tx)
                (requests-query context arguments value)
                {:row-fn request/row-fn})))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
