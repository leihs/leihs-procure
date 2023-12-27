(ns leihs.procurement.resources.requests
  (:require (clojure [set] [string])
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            (leihs.procurement.permissions [request-helpers :as request-perms]
                                           [requests :as requests-perms] [user :as user-perms])
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.resources.request-helpers :as request-helpers]
            [leihs.procurement.utils.helpers :refer [cast-uuids]]
            [leihs.procurement.utils.sql :as sqlp]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]]))

(defn create-order-status-enum-entries [order-stati]        ;;new
  (println ">oo> >here> to-name-and-lower-case-enums" order-stati)
  (map (fn [status] [:cast status :order_status_enum]) order-stati))

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
          [:or [:ilike :buildings.name term-percent]
           ;  [:ilike :procurement_requests.id term-percent]
           [:ilike :procurement_requests.short_id term-percent]
           [:ilike :procurement_requests.article_name term-percent]
           [:ilike :procurement_requests.article_number term-percent]
           [:ilike :procurement_requests.inspection_comment term-percent]
           [:ilike :procurement_requests.order_comment term-percent]
           [:ilike :procurement_requests.motivation term-percent]
           [:ilike :procurement_requests.receiver term-percent]
           [:ilike :procurement_requests.supplier_name term-percent]
           [:ilike :rooms.name term-percent]
           [:ilike :models.product term-percent]
           [:ilike :models.version term-percent]
           [:ilike :users.firstname term-percent]
           [:ilike :users.lastname term-percent]]))))

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
        start-sqlmap (-> (request/requests-base-query-with-state advanced-user?)
                         request-helpers/join-and-nest-associated-resources)]
    (cond-> start-sqlmap
            id (sql/where [:= :procurement_requests.id [:cast id :uuid]])
            ; short_id (sql/where [:in :procurement_requests.short_id short_id])
            category-id (-> (sql/where [:in :procurement_requests.category_id (cast-uuids category-id)])
                            (sqlp/merge-where-false-if-empty category-id))
            budget-period-id (-> (sql/where
                                   [:in :procurement_requests.budget_period_id (cast-uuids budget-period-id)])
                                 (sqlp/merge-where-false-if-empty budget-period-id))
            organization-id (-> (sql/where
                                  [:in :procurement_requests.organization_id (cast-uuids organization-id)])
                                (sqlp/merge-where-false-if-empty organization-id))
            priority (-> (sql/where [:in :procurement_requests.priority priority])
                         (sqlp/merge-where-false-if-empty priority))
            inspector-priority (-> (sql/where [:in :procurement_requests.inspector_priority inspector-priority])
                                   (sqlp/merge-where-false-if-empty inspector-priority))
            state (-> (sql/where
                        (request/get-where-conds-for-states state advanced-user?))
                      (sqlp/merge-where-false-if-empty state))
            order-status (-> (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries order-status)])
                             (sqlp/merge-where-false-if-empty order-status))
            requested-by-auth-user (sql/where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])
            search-term (search-query search-term))))

(defn get-requests
  [context arguments value]
  (let [ring-request (:request context)
        tx (:tx-next ring-request)
        auth-entity (:authenticated-entity ring-request)
        query (as-> context <>
                (requests-query-map <> arguments value)
                (requests-perms/apply-scope tx <> auth-entity)
                (sql-format <>))
        proc-requests (request/query-requests tx auth-entity query)]
    (->> proc-requests
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
    [:* :procurement_requests.price_cents <>]
    [:cast <> :bigint]
    [:sum <>]))

(defn total-price-sqlmap
  [qty-type bp-id]
  (-> (sql/select :procurement_requests.budget_period_id
                  [(sql-sum qty-type) :result])
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.budget_period_id [:cast bp-id :uuid]])
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
        (total-price-sqlmap [:cast bp-id :uuid])
        (sql/where [:= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))

(defn total-price-cents-inspected-requests
  [context _ value]
  (let [tx (-> context
               :request
               :tx-next)
        bp-id (:id value)]
    (-> [[:coalesce
          :procurement_requests.order_quantity
          :procurement_requests.approved_quantity]]
        (total-price-sqlmap bp-id)
        (sql/where [:!= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))

;[logbug.debug :as debug]
;(debug/debug-ns *ns*)