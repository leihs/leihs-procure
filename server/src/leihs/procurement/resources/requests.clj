(ns leihs.procurement.resources.requests
  (:require (clojure [set] [string])

    ;[clojure.java.jdbc :as jdbc]
            [honey.sql :refer [format] :rename {format sql-format}]


            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]
            (leihs.procurement.permissions [request-helpers :as request-perms]
                                           [requests :as requests-perms] [user :as user-perms])
            [leihs.procurement.resources.request :as request]


            [leihs.procurement.resources.request-helpers :as request-helpers]
            [leihs.procurement.utils.sql :as sqlp]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [debug error info spy warn]]
            ))

(defn create-order-status-enum-entries [order-stati]
  (println ">oo> enum" order-stati)
  (map (fn [status] [[:cast status :order_status_enum]]) order-stati)) ;;TODO

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







(defn cast-uuids [uuids]
  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))


(comment

  (let [

        ids ["123e4567-e89b-12d3-a456-426614174000", "123e4567-e89b-12d3-a456-426614174001"]

        p (println "a" cast-uuids)

        casted (cast-uuids ids)

        p (println "b" casted)

        ])

  )


(defn printer [name x]
  (println ">oo>" name x)
  x)


(defn requests-query-map                                    ;; TODO: FIXME
  [context arguments value]
  (let [id (:id arguments)
        ; short_id (:short_id arguments)
        p (println ">o> helper1")
        category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        p (println ">o> helper2" category-id budget-period-id organization-id)

        p (println ">oo> helper3a priority" (:priority arguments))
        priority (some->> arguments
                   :priority
                   (map request/to-name-and-lower-case))
        p (println ">oo> helper3b priority" priority)

        p (println ">oo> helper4a inspector-priority" (:inspector-priority arguments))
        inspector-priority (some->> arguments
                             :inspector_priority
                             (map request/to-name-and-lower-case))
        p (println ">oo> helper4b inspector-priority" inspector-priority)

        requested-by-auth-user (:requested_by_auth_user arguments)
        state (:state arguments)
        search-term (:search arguments)
        p (println ">o> helper5")

        p (println ">oo> helper5a :order_status" (:order_status arguments))
        order-status (some->> arguments :order_status (map request/to-name-and-lower-case))
        p (println ">oo> helper5b :order_status" order-status)


        p (println ">oo> order-status" order-status)

        rrequest (:request context)
        tx (:tx-next rrequest)
        p (println ">o> helper6")
        advanced-user? (user-perms/advanced? tx
                                             (:authenticated-entity rrequest))
        p (println ">o> helper7" advanced-user?)
        ;p (println ">requests-query-map>> ??")
        start-sqlmap (-> (request/requests-base-query-with-state advanced-user?)
                         request-helpers/join-and-nest-associated-resources)
        p (println ">o> helper8")
        ]

    (cond-> start-sqlmap
            id (sql/where [:in :procurement_requests.id id])
            ; short_id (sql/where [:in :procurement_requests.short_id short_id])
            category-id (-> (sql/where [:in :procurement_requests.category_id (cast-uuids category-id)])
                            ;category-id (-> (sql/where [:in :procurement_requests.category_id [:cast category-id :uuid]])
                            (sqlp/merge-where-false-if-empty category-id))
            budget-period-id (-> (sql/where
                                   [:in :procurement_requests.budget_period_id (cast-uuids budget-period-id)])
                                 (sqlp/merge-where-false-if-empty budget-period-id))
            organization-id (-> (sql/where
                                  ;[:in :procurement_requests.organization_id organization-id])
                                  [:in :procurement_requests.organization_id (cast-uuids organization-id)])
                                (sqlp/merge-where-false-if-empty organization-id))



            priority (-> (sql/where [:in :procurement_requests.priority priority])
                         (sqlp/merge-where-false-if-empty priority))

            ;true (printer ("helper5b sql/priority" priority))

            inspector-priority (-> (sql/where [:in :procurement_requests.inspector_priority inspector-priority])
                                   (sqlp/merge-where-false-if-empty inspector-priority))
            ;true (printer ("helper5b sql/inspector-priority" inspector-priority))


            state (-> (sql/where
                        (request/get-where-conds-for-states state advanced-user?))
                      (sqlp/merge-where-false-if-empty state))
            ;true (printer ("helper5b sql/state" state))



            order-status (-> (sql/where [:in :procurement_requests.order_status
                                         ;(map [[#(:cast % :order_status_enum)]] order-status)
                                         (create-order-status-enum-entries order-status) ;; TODO: cast enum
                                         ])
                             ;(map #(sql/call :cast % :order_status_enum) order-status)]) ;; TODO: original, FIXME
                             (sqlp/merge-where-false-if-empty order-status))
            ;true (printer ("helper5b sql/order-status" order-status))


            requested-by-auth-user (sql/where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])

            search-term (search-query search-term))))






;(ns leihs.my.back.html
;    (:refer-clojure :exclude [keyword str])
;    (:require
;      [hiccup.page :refer [html5]]
;      [honey.sql :refer [format] :rename {format sql-format}]
;      [honey.sql.helpers :as sql]
;      [leihs.core.http-cache-buster2 :as cache-buster]
;      [leihs.core.json :refer [to-json]]
;      [leihs.core.remote-navbar.shared :refer [navbar-props]]
;      [leihs.core.shared :refer [head]]
;      [leihs.core.url.core :as url]
;      [leihs.my.authorization :as auth]
;      [leihs.core.db :as db]
;      [next.jdbc :as jdbc]))





(comment
  (let [
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds)

        raw-order-status '[NOT_PROCESSED IN_PROGRESS PROCURED ALTERNATIVE_PROCURED NOT_PROCURED]
        p (println ">o> raw-order-status" raw-order-status)

        order-status (some->> raw-order-status
                       (map request/to-name-and-lower-case))
        p (println ">o> order-status" order-status)

        os-map (create-order-status-enum-entries order-status)
        p (println ">o> order-os-map" os-map)

        sql (-> (sql/select :*)
                (sql/from :procurement_requests)
                (sql/where [:in :procurement_requests.order_status
                            os-map

                            ;[[ [:cast (first order-status) :order_status_enum]]] ;;works, 1 entry
                            ;[[ [:cast (second order-status) :order_status_enum]]] ;;works, no entry

                            ])
                )

        p (println "\nsql" sql)
        query (sql-format sql)

        p (println "\nquery" query)
        p (println "\nresult" (jdbc/execute! tx query))]
    )
  )













(defn get-requests
  [context arguments value]
  (let [ring-request (:request context)
        tx (:tx-next ring-request)
        auth-entity (:authenticated-entity ring-request)
        query (as-> context <>
                (requests-query-map <> arguments value)
                (requests-perms/apply-scope tx <> auth-entity)
                (sql-format <>))

        p (println ">o> abc" query)

        proc-requests (request/query-requests tx auth-entity query)]
    (->>
      proc-requests
      (map (fn [proc-req]
             (as-> proc-req <>
               (request-perms/apply-permissions tx
                                                auth-entity
                                                <>
                                                #(assoc % :request-id (:id <>)))
               (request-perms/add-action-permissions <>)))))))



;(defn get-requests-DEV
;  [context arguments value]
;
;  (println ">>>requests::get-requests ======================")
;  (println ">>>args" arguments)
;  (println ">>>value" value)
;
;  (let [ring-request (:request context)
;        tx (:tx-next ring-request)
;        auth-entity (:authenticated-entity ring-request)
;        p (println ">>>auth-entity")
;        p (println ">>>get-requests>> before first")
;
;        query (let [query (as-> context <>
;                            (do (println ">o> 1-After requests-query-map:")
;                                ;(do (println ">o> 1-After requests-query-map:" <>)
;                                (requests-query-map <> arguments value))
;
;                            ;>o> 2-After apply-scope: {:select (nil [(:case [:= :procurement_requests.approved_quantity nil] NEW [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] APPROVED [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] PARTIALLY_APPROVED [:= :procurement_requests.approved_quantity 0] DENIED) :state] [#sql/call [:row_to_json :procurement_budget_periods_2] :budget_period] nil [#sql/call [:row_to_json :models] :model] nil [#sql/call [:row_to_json :procurement_templates] :template] nil [#sql/call [:row_to_json :suppliers] :supplier] [#sql/call [:row_to_json :users] :user]), :from [:procurement_requests], :left-join (:models [:= :models.id :procurement_requests.model_id] :procurement_templates [:= :procurement_templates.id :procurement_requests.template_id] :suppliers [:= :suppliers.id :procurement_requests.supplier_id] :users [:= :users.id :procurement_requests.user_id]), :order-by [nil], :join [:procurement_budget_periods [:= :procurement_budget_periods.id :procurement_requests.budget_period_id] [{:select [:* [:end_date :is_past]], :from [:procurement_budget_periods]} :procurement_budget_periods_2] [:= :procurement_budget_periods_2.id :procurement_requests.budget_period_id] :procurement_categories [:= :procurement_categories.id :procurement_requests.category_id] :procurement_main_categories [:= :procurement_main_categories.id :procurement_categories.main_category_id] :procurement_organizations [:= :procurement_organizations.id :procurement_requests.organization_id] [:procurement_organizations :procurement_departments] [:= :procurement_departments.id :procurement_organizations.parent_id] :rooms [:= :rooms.id :procurement_requests.room_id] :buildings [:= :buildings.id :rooms.building_id]], :where [:and [:in :procurement_requests.category_id [#uuid "7d5ba731-edd9-41ba-8773-7337d24c2327"]] [:in :procurement_requests.budget_period_id [#uuid "8b8fe440-cae5-4bf9-8048-d0ec2399faa1"]] [:in :procurement_requests.organization_id [#uuid "fb664326-a8ef-4556-af02-07d3127cd9ec"]] [:in :procurement_requests.priority (normal high)] [:in :procurement_requests.inspector_priority (low medium high mandatory)] [:or [:= :procurement_requests.approved_quantity nil] [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] [:= :procurement_requests.approved_quantity 0]]]}
;                            ;                                   ^  TODO:
;
;                            (do (println ">o> 2-After apply-scope:" <>)
;                                (requests-perms/apply-scope tx <> auth-entity))
;                            (do (println ">o> 3-After sql-format:" <>)
;                                (spy (sql-format <>))))] query
;                                                         ;; Execute the query with `query`, for example using a PostgreSQL function.
;                                                         ;; Example: (execute-postgres-query tx query)
;                                                         )
;
;        p (spy query)
;
;
;        p (println ">o>ring-request-3" query)
;
;        ;p (println ">>broken-query" (spy query))            ;;TODO: log broken query
;        ;p (throw "my-log-error")
;
;        proc-requests (request/query-requests tx auth-entity query)
;
;
;        p (println ">>>ring-request-procRequests" proc-requests)
;
;        ]
;    (->>
;      proc-requests
;      (map (fn [proc-req]
;             (as-> proc-req <>
;               (request-perms/apply-permissions tx
;                                                auth-entity
;                                                <>
;                                                #(assoc % :request-id (:id <>)))
;               (request-perms/add-action-permissions <>)))))))








(defn get-total-price-cents
  [tx sqlmap]

  (println ">oo> get-total-price-cents" sqlmap)


  (or (some->> sqlmap
        sql-format
        (jdbc/execute-one! tx)
        :result)
      0))

(defn- sql-sum
  [qty-type]

  (println ">oo> sql-sum" qty-type)


  (spy (as-> qty-type <>
         (:call :* :procurement_requests.price_cents <>)
         (:call :cast <> :bigint)
         (:call :sum <>)))
  )


(comment

  (let [
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds-next)
        query (sql-format {:select :*
                           :from [:users]
                           :where [:= :id [:cast user-id :uuid]]})

        query2 (-> (sql/select :*)
                   (sql/from :users)
                   (sql/where [:= :id user-id])
                   sql-format
                   (->> (jdbc/execute! tx))
                   )

        p (println "\nquery" query)
        p (println "\nquery2" query2)
        ]

    )
  )

(defn total-price-sqlmap
  [qty-type bp-id]

  (println ">oo> total-price-sqlmap" qty-type)

  (spy (-> (sql/select :procurement_requests.budget_period_id
                       [(sql-sum qty-type) :result])
           (sql/from :procurement_requests)
           (sql/where [:= :procurement_requests.budget_period_id [:cast bp-id :uuid]])
           (sql/group-by :procurement_requests.budget_period_id)))
  )

(defn specific-total-price-cents
  [tx qty-type bp-id]

  (println ">oo> specific-total-price-cents" qty-type bp-id)


  (->> bp-id
       (total-price-sqlmap qty-type)
       (get-total-price-cents tx)))

(defn total-price-cents-requested-quantities
  [context _ value]

  (println ">oo> total-price-cents-requested-quantities" value)
  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.requested_quantity
                              (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (println ">oo> total-price-cents-approved-quantities" value)

  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.approved_quantity
                              (:id value)))



(defn total-price-cents-order-quantities
  [context _ value]

  (println ">oo> total-price-cents-order-quantities" value)

  (specific-total-price-cents (-> context
                                  :request
                                  :tx-next)
                              :procurement_requests.order_quantity
                              (:id value)))




(defn total-price-cents-new-requests
  [context _ value]

  (println ">oo> total-price-cents-new-requests" value)


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
  (println ">oo> total-price-cents-inspected-requests" value)

  (let [tx (-> context
               :request
               :tx-next)
        bp-id (:id value)

        p (println ">oo> bp-id" bp-id)
        ]


    (-> (:call :coalesce                                    ;;FIXME TODO
          :procurement_requests.order_quantity
          :procurement_requests.approved_quantity)
        (total-price-sqlmap bp-id)
        ;(total-price-sqlmap [:cast bp-id :uuid])
        (sql/where [:!= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))

    ))
