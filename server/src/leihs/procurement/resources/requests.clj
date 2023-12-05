(ns leihs.procurement.resources.requests
  (:require [clojure set string]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions [request-helpers :as request-perms]
             [requests :as requests-perms] [user :as user-perms]]

            [taoensso.timbre :refer [debug info warn error spy]]


            [leihs.core.db :as db]



            [leihs.procurement.resources.request :as request]
            [leihs.procurement.resources.request-helpers :as request-helpers]
            [leihs.procurement.utils.sql :as sql]))

(defn search-query
  [sql-query term]
  (let [term-percent (str "%" term "%")]
    (-> sql-query
        ; NOTE: everything merged already
        ; (sql/merge-join :rooms [:= :procurement_requests.room_id :rooms.id])
        ; (sql/merge-join :buildings [:= :rooms.building_id :buildings.id])
        ; (sql/merge-join :users [:= :procurement_requests.user_id :users.id])
        ; NOTE: models are joined in the base-query already
        (sql/merge-where
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

(defn printer [name x]
  (println ">oo>" name x)
  x)

(defn requests-query-map
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
        ;order-status (some->> arguments :order_status (map request/to-name-and-upper-case))
        order-status (some->> arguments :order_status (map request/to-name-and-lower-case))
        p (println ">oo> helper5b :order_status" order-status)


        p (println ">oo> order-status" order-status)

        rrequest (:request context)
        tx (:tx rrequest)
        advanced-user? (spy (user-perms/advanced? tx
                                                  (:authenticated-entity rrequest)))

        p (println ">o> helper7" advanced-user?)

        start-sqlmap (spy (-> (request/requests-base-query-with-state advanced-user?)
                              request-helpers/join-and-nest-associated-resources))


        p (println ">o> helper8")
        p (println ">oo> helper8" order-status inspector-priority priority)


        ;>o> searchTerm::before    >  <
        ;>o> searchTerm::before    > java.lang.String <
        ;>o> searchTerm::before    > 0 <
        ;>o> requests::search-query %%
        ;>o> searchTerm::test-query    > [SELECT * FROM buildings, procurement_requests, rooms, models, users WHERE (?, buildings.name, ?) OR (?, users.lastname, ?) ~~* %% ~~* %%] <


        p (println ">o> searchTerm::before    >" search-term "<")
        p (println ">o> searchTerm::before    >" (class search-term) "<")
        p (println ">o> searchTerm::before    >" (count search-term) "<")

        test (search-query (-> (sql/select :*)
                               (sql/from :buildings :procurement_requests :rooms :models :users)) search-term)
        test (-> test
                 sql/format
                 )
        p (println ">o> searchTerm::test-query    >" test "<")

        ]

    (cond-> start-sqlmap
            id (sql/merge-where [:in :procurement_requests.id id])
            ; short_id (sql/merge-where [:in :procurement_requests.short_id short_id])
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
            order-status (-> (sql/merge-where [:in :procurement_requests.order_status
                                               (map #(sql/call :cast % :order_status_enum) order-status)])
                             (sql/merge-where-false-if-empty order-status))
            requested-by-auth-user (sql/merge-where [:= :procurement_requests.user_id
                                                     (-> context
                                                         :request
                                                         :authenticated-entity
                                                         :user_id)])
            search-term (search-query search-term))))


(comment
  (let [
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds)

        raw-order-status '[NOT_PROCESSED IN_PROGRESS PROCURED ALTERNATIVE_PROCURED NOT_PROCURED]
        p (println ">o> raw-order-status" raw-order-status)

        order-status (some->> raw-order-status
                       (map request/to-name-and-lower-case))

        p (println ">o> order-status" order-status)

        os-map (map #(sql/call :cast % :order_status_enum) order-status)
        p (println ">o> order-os-map" os-map)
        ;>o> order-os-map (#sql/call [:cast not_processed :order_status_enum] #sql/call [:cast in_progress :order_status_enum] #sql/call [:cast procured :order_status_enum] #sql/call [:cast alternative_procured :order_status_enum] #sql/call [:cast not_procured :order_status_enum])


        sql (-> (sql/select :*)
                (sql/from :procurement_requests)
                (sql/where [:in :procurement_requests.order_status
                            (map #(sql/call :cast % :order_status_enum) order-status)
                            ])
                )

        p (println "\nsql" sql)
        query (sql/format sql)

        p (println "\nquery" query)
        p (println "\nresult" (jdbc/query tx query))]
    )
  )

(defn get-requests
  [context arguments value]

  ;(spy context)
  (spy arguments)
  (spy value)

  (let [ring-request (:request context)
        tx (:tx ring-request)
        auth-entity (:authenticated-entity ring-request)
        p (println ">get-requests>> ???")
        ;p  (spy context)



        query (let [query (as-> context <>
                            (do (println ">o> 1-After requests-query-map:")
                                ;(do (println ">o> 1-After requests-query-map:" <>)
                                (requests-query-map <> arguments value))

                            ;>o> 2-After apply-scope: {:select (nil [(:case [:= :procurement_requests.approved_quantity nil] NEW [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] APPROVED [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] PARTIALLY_APPROVED [:= :procurement_requests.approved_quantity 0] DENIED) :state] [#sql/call [:row_to_json :procurement_budget_periods_2] :budget_period] nil [#sql/call [:row_to_json :models] :model] nil [#sql/call [:row_to_json :procurement_templates] :template] nil [#sql/call [:row_to_json :suppliers] :supplier] [#sql/call [:row_to_json :users] :user]), :from [:procurement_requests], :left-join (:models [:= :models.id :procurement_requests.model_id] :procurement_templates [:= :procurement_templates.id :procurement_requests.template_id] :suppliers [:= :suppliers.id :procurement_requests.supplier_id] :users [:= :users.id :procurement_requests.user_id]), :order-by [nil], :join [:procurement_budget_periods [:= :procurement_budget_periods.id :procurement_requests.budget_period_id] [{:select [:* [:end_date :is_past]], :from [:procurement_budget_periods]} :procurement_budget_periods_2] [:= :procurement_budget_periods_2.id :procurement_requests.budget_period_id] :procurement_categories [:= :procurement_categories.id :procurement_requests.category_id] :procurement_main_categories [:= :procurement_main_categories.id :procurement_categories.main_category_id] :procurement_organizations [:= :procurement_organizations.id :procurement_requests.organization_id] [:procurement_organizations :procurement_departments] [:= :procurement_departments.id :procurement_organizations.parent_id] :rooms [:= :rooms.id :procurement_requests.room_id] :buildings [:= :buildings.id :rooms.building_id]], :where [:and [:in :procurement_requests.category_id [#uuid "7d5ba731-edd9-41ba-8773-7337d24c2327"]] [:in :procurement_requests.budget_period_id [#uuid "8b8fe440-cae5-4bf9-8048-d0ec2399faa1"]] [:in :procurement_requests.organization_id [#uuid "fb664326-a8ef-4556-af02-07d3127cd9ec"]] [:in :procurement_requests.priority (normal high)] [:in :procurement_requests.inspector_priority (low medium high mandatory)] [:or [:= :procurement_requests.approved_quantity nil] [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] [:= :procurement_requests.approved_quantity 0]]]}
                            ;                                   ^  TODO:

                            ; TODO/GOOD >o> 2-After apply-scope: {:select (#sql/raw DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.* [#sql/call [:case [:= :procurement_requests.approved_quantity nil] NEW [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] APPROVED [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] PARTIALLY_APPROVED [:= :procurement_requests.approved_quantity 0] DENIED] :state] [#sql/call [:row_to_json :procurement_budget_periods_2] :budget_period] #sql/raw row_to_json(procurement_categories)::jsonb || jsonb_build_object('main_category', row_to_json(procurement_main_categories)) AS category [#sql/call [:row_to_json :models] :model] #sql/raw row_to_json(procurement_organizations)::jsonb || jsonb_build_object('department', row_to_json(procurement_departments)) AS organization [#sql/call [:row_to_json :procurement_templates] :template] #sql/raw row_to_json(rooms)::jsonb || jsonb_build_object('building', row_to_json(buildings)) AS room [#sql/call [:row_to_json :suppliers] :supplier] [#sql/call [:row_to_json :users] :user]), :from (:procurement_requests), :left-join (:models [:= :models.id :procurement_requests.model_id] :procurement_templates [:= :procurement_templates.id :procurement_requests.template_id] :suppliers [:= :suppliers.id :procurement_requests.supplier_id] :users [:= :users.id :procurement_requests.user_id]), :order-by (#sql/raw concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))), :join (:procurement_budget_periods [:= :procurement_budget_periods.id :procurement_requests.budget_period_id] [{:select (:* [#sql/call [:> :current_date :end_date] :is_past]), :from (:procurement_budget_periods)} :procurement_budget_periods_2] [:= :procurement_budget_periods_2.id :procurement_requests.budget_period_id] :procurement_categories [:= :procurement_categories.id :procurement_requests.category_id] :procurement_main_categories [:= :procurement_main_categories.id :procurement_categories.main_category_id] :procurement_organizations [:= :procurement_organizations.id :procurement_requests.organization_id] [:procurement_organizations :procurement_departments] [:= :procurement_departments.id :procurement_organizations.parent_id] :rooms [:= :rooms.id :procurement_requests.room_id] :buildings [:= :buildings.id :rooms.building_id]), :where [:and [:in :procurement_requests.category_id [7d5ba731-edd9-41ba-8773-7337d24c2327]] [:in :procurement_requests.budget_period_id [8b8fe440-cae5-4bf9-8048-d0ec2399faa1]] [:in :procurement_requests.organization_id [fb664326-a8ef-4556-af02-07d3127cd9ec]] [:in :procurement_requests.priority (normal high)] [:in :procurement_requests.inspector_priority (low medium high mandatory)] [:or [:= :procurement_requests.approved_quantity nil] [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] [:= :procurement_requests.approved_quantity 0]] [:in :procurement_requests.order_status (#sql/call [:cast not_processed :order_status_enum] #sql/call [:cast in_progress :order_status_enum] #sql/call [:cast procured :order_status_enum] #sql/call [:cast alternative_procured :order_status_enum] #sql/call [:cast not_procured :order_status_enum])]]}


                            (do (println ">o> 2-After apply-scope:" <>)
                                (requests-perms/apply-scope tx <> auth-entity))
                            (do (println ">o> 3-After sql-format:" <>)
                                (sql/format <>)))] query
                                                   ;; Execute the query with `query`, for example using a PostgreSQL function.
                                                   ;; Example: (execute-postgres-query tx query)
                                                   )

        p (spy query)

        p (println ">>broken-query" (spy query))            ;;TODO: log broken query
        ;p (throw "my-log-error")
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
                              :procurement_requests.requested_quantity
                              (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :procurement_requests.approved_quantity
                              (:id value)))

(defn total-price-cents-order-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :procurement_requests.order_quantity
                              (:id value)))

(defn total-price-cents-new-requests
  [context _ value]
  (let [tx (-> context
               :request
               :tx)
        bp-id (:id value)]
    (-> :requested_quantity
        (total-price-sqlmap bp-id)
        (sql/merge-where [:= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))

(defn total-price-cents-inspected-requests
  [context _ value]
  (let [tx (-> context
               :request
               :tx)
        bp-id (:id value)]
    (-> (sql/call :coalesce
                  :procurement_requests.order_quantity
                  :procurement_requests.approved_quantity)
        (total-price-sqlmap bp-id)
        (sql/merge-where [:!= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))))
