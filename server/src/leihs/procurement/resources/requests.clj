(ns leihs.procurement.resources.requests
  (:require [clojure set string]

    ;[clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sqlp]

            [taoensso.timbre :refer [debug info warn error spy]]


            [honey.sql :refer [format] :rename {format sql-format}]
            [leihs.core.db :as db]
            [next.jdbc :as jdbc]
            [honey.sql.helpers :as sql]


            [clojure.tools.logging :as log]
            [leihs.procurement.permissions [request-helpers :as request-perms]
             [requests :as requests-perms] [user :as user-perms]]
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.resources.request-helpers :as request-helpers]
            ))

(defn search-query
  [sql-query term]
  (let [term-percent (str "%" term "%")]
    (-> sql-query
        ; NOTE: everything merged already
        ; (sql/join :rooms [:= :procurement_requests.room_id :rooms.id])
        ; (sql/join :buildings [:= :rooms.building_id :buildings.id])
        ; (sql/join :users [:= :procurement_requests.user_id :users.id])
        ; NOTE: models are joined in the base-query already
        (sql/where
          [:or ["~~*" :buildings.name term-percent]
           ;  ["~~*" :procurement_requests.id term-percent]
           ["~~*" :procurement_requests.short_id term-percent]
           ["~~*" :procurement_requests.article_name term-percent]
           ["~~*" :procurement_requests.article_number term-percent]
           ["~~*" :procurement_requests.inspection_comment term-percent]
           ["~~*" :procurement_requests.order_comment term-percent]
           ["~~*" :procurement_requests.motivation term-percent]
           ["~~*" :procurement_requests.receiver term-percent]
           ["~~*" :procurement_requests.supplier_name term-percent]
           ["~~*" :rooms.name term-percent]
           ["~~*" :models.product term-percent]
           ["~~*" :models.version term-percent]
           ["~~*" :users.firstname term-percent]
           ["~~*" :users.lastname term-percent]]))))

(defn requests-query-map
  [context arguments value]
  (let [id (:id arguments)
        ; short_id (:short_id arguments)
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
        state (:state arguments)
        search-term (:search arguments)
        order-status (some->> arguments :order_status (map request/to-name-and-lower-case))
        rrequest (:request context)
        tx (:tx-next rrequest)
        advanced-user? (user-perms/advanced? tx
                                             (:authenticated-entity rrequest))
        p (println ">requests-query-map>> ??")
        start-sqlmap (-> (request/requests-base-query-with-state advanced-user?)
                         request-helpers/join-and-nest-associated-resources)]
    (cond-> start-sqlmap
            id (sql/where [:in :procurement_requests.id id])
            ; short_id (sql/where [:in :procurement_requests.short_id short_id])
            category-id (-> (sql/where [:in :procurement_requests.category_id
                                        category-id])
                            (sqlp/merge-where-false-if-empty category-id))
            budget-period-id (-> (sql/where
                                   [:in :procurement_requests.budget_period_id
                                    budget-period-id])
                                 (sqlp/merge-where-false-if-empty budget-period-id))
            organization-id (-> (sql/where
                                  [:in :procurement_requests.organization_id
                                   organization-id])
                                (sqlp/merge-where-false-if-empty organization-id))
            priority (-> (sql/where [:in :procurement_requests.priority
                                     priority])
                         (sqlp/merge-where-false-if-empty priority))
            inspector-priority
            (-> (sql/where [:in :procurement_requests.inspector_priority
                            inspector-priority])
                (sqlp/merge-where-false-if-empty inspector-priority))
            state (-> (sql/where
                        (request/get-where-conds-for-states state advanced-user?))
                      (sqlp/merge-where-false-if-empty state))
            order-status (-> (sql/where [:in :procurement_requests.order_status
                                         (map #(:cast % :order_status_enum) order-status)])
                             (sqlp/merge-where-false-if-empty order-status))
            requested-by-auth-user (sql/where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])
            search-term (search-query search-term))))

(defn get-requests
  [context arguments value]

  ;(spy context)
  (spy arguments)
  (spy value)

  (let [ring-request (:request context)
        tx (:tx-next ring-request)
        auth-entity (:authenticated-entity ring-request)
        p (println ">get-requests>> ???")
        ;p  (spy context)
        query (as-> context <>
                ;(spy (requests-query-map <> arguments value))
                ;(spy (requests-perms/apply-scope tx <> auth-entity))
                (sql-format <>))

        p (println ">>>ring-request-1" query)

        query (as-> context <>
                (requests-query-map <> arguments value)
                ;(spy (requests-perms/apply-scope tx <> auth-entity))
                (sql-format <>))
        p (println ">>>ring-request-2" query)

        query (as-> context <>
                (requests-query-map <> arguments value)
                (requests-perms/apply-scope tx <> auth-entity)
                (sql-format <>))

        p (println ">>>ring-request-2" query)

        ;p (println ">>broken-query" (spy query))            ;;TODO: log broken query
        ;p (throw "my-log-error")

        proc-requests (request/query-requests tx auth-entity query)


        p (println ">>>ring-request-procRequests" proc-requests)

        ]
    (->>
      proc-requests
      (map (fn [proc-req]
             (as-> proc-req <>
               (request-perms/apply-permissions tx
                                                auth-entity
                                                <>
                                                #(assoc % :request-id (:id <>)))
               (request-perms/add-action-permissions <>)))))))

(defn get-total-price-cents
  [tx sqlmap]
  (or (some->> sqlmap
        sql-format
        (jdbc/execute-one! tx)

        :result)
      0))

(defn- sql-sum
  [qty-type]
  (as-> qty-type <>
    (:call :* :procurement_requests.price_cents <>)
    (:call :cast <> :bigint)
    (:call :sum <>)))

(defn total-price-sqlmap
  [qty-type bp-id]
  (-> (sql/select :procurement_requests.budget_period_id
                  [(sql-sum qty-type) :result])
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.budget_period_id bp-id])
      (sql/group-by :procurement_requests.budget_period_id)))

(defn specific-total-price-cents
  [tx qty-type bp-id]
  (->> bp-id
       (total-price-sqlmap qty-type)
       (get-total-price-cents tx)))

(defn total-price-cents-requested-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.requested_quantity
                              (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.approved_quantity
                              (:id value)))

(defn total-price-cents-order-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.order_quantity
                              (:id value)))

(defn total-price-cents-new-requests
  [context _ value]
  (let [tx (-> context
               :request
               :tx-next)
        bp-id (:id value)]
    (-> :requested_quantity
        (total-price-sqlmap bp-id)
        (sql/where [:= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))

(defn total-price-cents-inspected-requests
  [context _ value]
  (let [tx (-> context
               :request
               :tx-next)
        bp-id (:id value)]
    (-> (:call :coalesce
          :procurement_requests.order_quantity
          :procurement_requests.approved_quantity)
        (total-price-sqlmap bp-id)
        (sql/where [:!= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))
