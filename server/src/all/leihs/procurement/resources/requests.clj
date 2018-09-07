(ns leihs.procurement.resources.requests
  (:require [clojure set string]
            [clojure.contrib.seq :refer [find-first]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions [request-helpers :as request-perms]
             [requests :as requests-perms] [user :as user-perms]]
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.utils.sql :as sql]))

(defn search-query
  [sql-query term]
  (let [term-percent (str "%" term "%")]
    (->
      sql-query
      (sql/merge-join :rooms [:= :procurement_requests.room_id :rooms.id])
      (sql/merge-join :buildings [:= :rooms.building_id :buildings.id])
      (sql/merge-join :users [:= :procurement_requests.user_id :users.id])
      (sql/merge-where
        [:or ["~~*" :buildings.name term-percent]
         ["~~*" :procurement_requests.article_name term-percent]
         ["~~*" :procurement_requests.article_number term-percent]
         ["~~*" :procurement_requests.inspection_comment term-percent]
         ["~~*" :procurement_requests.motivation term-percent]
         ["~~*" :procurement_requests.receiver term-percent]
         ["~~*" :procurement_requests.supplier_name term-percent]
         ["~~*" :rooms.name term-percent] ["~~*" :users.firstname term-percent]
         ["~~*" :users.lastname term-percent]]))))

(defn requests-query-map
  [context arguments value]
  (let [id (:id arguments)
        category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        priority (some->> arguments
                          :priority
                          (map request/to-name-and-lower-case))
        inspector-priority (some->> arguments
                                    :inspector_priority
                                    (map request/to-name-and-lower-case))
        requested-by-auth-user (:requested_by_auth_user arguments)
        from-categories-of-auth-user (:from_categories_of_auth_user arguments)
        state (:state arguments)
        search-term (:search arguments)
        rrequest (:request context)
        tx (:tx rrequest)
        advanced-user? (user-perms/advanced? tx
                                             (:authenticated-entity rrequest))
        start-sqlmap (request/requests-base-query-with-state advanced-user?)]
    (cond-> start-sqlmap
      id (sql/merge-where [:in :procurement_requests.id id])
      category-id (-> (sql/merge-where [:in :procurement_requests.category_id
                                        category-id])
                      (sql/merge-where-false-if-empty category-id))
      budget-period-id (-> (sql/merge-where
                             [:in :procurement_requests.budget_period_id
                              budget-period-id])
                           (sql/merge-where-false-if-empty budget-period-id))
      organization-id (-> (sql/merge-where
                            [:in :procurement_requests.organization_id
                             organization-id])
                          (sql/merge-where-false-if-empty organization-id))
      priority (-> (sql/merge-where [:in :procurement_requests.priority
                                     priority])
                   (sql/merge-where-false-if-empty priority))
      inspector-priority
        (-> (sql/merge-where [:in :procurement_requests.inspector_priority
                              inspector-priority])
            (sql/merge-where-false-if-empty inspector-priority))
      state (-> (sql/merge-where
                  (request/get-where-conds-for-states state advanced-user?))
                (sql/merge-where-false-if-empty state))
      requested-by-auth-user (sql/merge-where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])
      from-categories-of-auth-user
        (sql/merge-where
          [:in :procurement_requests.category_id
           (-> (sql/select :category_id)
               (sql/from :procurement_category_inspectors)
               (sql/merge-where [:= :procurement_category_inspectors.user_id
                                 (-> context
                                     :request
                                     :authenticated-entity
                                     :id)]))])
      search-term (search-query search-term))))

(defn get-requests
  [context arguments value]
  (let [ring-request (:request context)
        tx (:tx ring-request)
        auth-entity (:authenticated-entity ring-request)
        query (as-> context <>
                (requests-query-map <> arguments value)
                (requests-perms/apply-scope tx <> auth-entity)
                (sql/format <>))
        proc-requests (request/query-requests tx query)]
    (->> proc-requests
         (map request/reverse-exchange-attrs)
         (map (fn [proc-req]
                (request-perms/apply-permissions
                  tx
                  auth-entity
                  proc-req
                  #(assoc % :request-id (:id proc-req))))))))

(defn get-total-price-cents
  [tx sqlmap]
  (or (some->> sqlmap
               sql/format
               (jdbc/query tx)
               first
               :result)
      0))

(defn- sql-sum
  [qty-type]
  (as-> qty-type <>
    (name <>)
    (str "procurement_requests." <>)
    (keyword <>)
    (sql/call :* :procurement_requests.price_cents <>)
    (sql/call :cast <> :bigint)
    (sql/call :sum <>)))

(defn total-price-sqlmap
  [qty-type bp-id]
  (-> (sql/select :procurement_requests.budget_period_id
                  [(sql-sum qty-type) :result])
      (sql/from :procurement_requests)
      (sql/merge-where [:= :procurement_requests.budget_period_id bp-id])
      (sql/group :procurement_requests.budget_period_id)))

(defn specific-total-price-cents
  [tx qty-type bp-id]
  (->> bp-id
       (total-price-sqlmap qty-type)
       (get-total-price-cents tx)))

(defn total-price-cents-requested-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :requested_quantity
                              (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :approved_quantity
                              (:id value)))

(defn total-price-cents-order-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :order_quantity
                              (:id value)))
