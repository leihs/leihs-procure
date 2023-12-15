(ns leihs.procurement.resources.requests
  (:require (clojure [set] [string])

    ;[clojure.java.jdbc :as jdbc]
            [honey.sql :refer [format] :rename {format sql-format}]

            [leihs.procurement.utils.helpers :refer [add-comment-to-sql-format cast-uuids]]

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

;(defn create-order-status-enum-entries [order-stati]
;  (println ">oo> enum" order-stati)
;  (map (fn [status] [[:cast status :order_status_enum]]) order-stati)) ;;TODO

(defn create-order-status-enum-entries [order-stati]        ;;new
  (println ">oo> >here> to-name-and-lower-case-enums" order-stati)
  (map (fn [status] [:cast status :order_status_enum]) order-stati))

(defn search-query
  [sql-query term]

  (println ">oo> search-query" term)


  (let [term-percent (str "%" term "%")
        p (println ">o> requests::search-query" term-percent)

        ]
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







;(defn cast-uuids [uuids]
;  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))


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

  (println ">oo> requests-query-map" value)


  (let [id (:id arguments)
        ; short_id (:short_id arguments)
        p (println ">o> helper1 id=" id)
        category-id (:category_id arguments)
        budget-period-id (:budget_period_id arguments)
        organization-id (:organization_id arguments)
        p (println ">o> helper2" category-id budget-period-id organization-id)

        p (println ">oo> helper3a priority" (:priority arguments))
        priority (some->> arguments
                   :priority
                   ;(map request/to-name-and-upper-case))
                   (map request/to-name-and-lower-case))
        p (println ">oo> helper3b priority" priority)

        p (println ">oo> helper4a inspector-priority" (:inspector-priority arguments))
        inspector-priority (some->> arguments
                             :inspector_priority
                             ;(map request/to-name-and-upper-case))
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
        p (println ">oo> helper5c :order_status" (class (first order-status)))


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
        p (println ">oo> helper8" order-status inspector-priority priority)
        p (println ">oo> helper9" start-sqlmap)


        ;>o> searchTerm::before    >  <
        ;>o> searchTerm::before    > java.lang.String <
        ;>o> searchTerm::before    > 0 <
        ;>o> requests::search-query %%
        ;>o> searchTerm::test-query    > [SELECT * FROM buildings, procurement_requests, rooms, models, users WHERE (?, buildings.name, ?) OR (?, users.lastname, ?) ~~* %% ~~* %%] <


        ;p (println ">o> searchTerm::before    >" search-term "<")
        ;p (println ">o> searchTerm::before    >" (class search-term) "<")
        ;p (println ">o> searchTerm::before    >" (count search-term) "<")
        ;
        ;test (search-query (-> (sql/select :*)
        ;                       (sql/from :buildings :procurement_requests :rooms :models :users)) search-term)
        ;test (-> test
        ;         sql-format
        ;         ;(sql-format :quoted true)
        ;         )
        ;p (println ">o> searchTerm::test-query    >" test "<")

        ;p (println ">o> searchTerm::after" (jdbc/execute! tx test))

        p (println ">o>> where [:in :procurement_requests.id [:cast id :uuid]])" id)
        ]

    (cond-> start-sqlmap
            id (sql/where [:= :procurement_requests.id [:cast id :uuid]])
            ; short_id (sql/where [:in :procurement_requests.short_id short_id])


            ;:cause Der in SQL für eine Instanz von leihs.procurement.resources.requests$cast_uuids zu verwendende Datentyp kann nicht abgeleitet werden. Benutzen Sie 'setObject()' mit einem expliziten Typ, um ihn festzulegen.


            ;; OK START
            category-id (-> (sql/where [:in :procurement_requests.category_id (cast-uuids category-id)])
                            (sqlp/merge-where-false-if-empty category-id))

            budget-period-id (-> (sql/where
                                   [:in :procurement_requests.budget_period_id (cast-uuids budget-period-id)])
                                 (sqlp/merge-where-false-if-empty budget-period-id))

            organization-id (-> (sql/where
                                  [:in :procurement_requests.organization_id (cast-uuids organization-id)])
                                (sqlp/merge-where-false-if-empty organization-id))
            ;; OK END


            priority (-> (sql/where [:in :procurement_requests.priority priority])
                         (sqlp/merge-where-false-if-empty priority))


            inspector-priority (-> (sql/where [:in :procurement_requests.inspector_priority inspector-priority])
                                   (sqlp/merge-where-false-if-empty inspector-priority))

            ;; FIXED
            state (-> (sql/where
                        (request/get-where-conds-for-states state advanced-user?))
                      (sqlp/merge-where-false-if-empty state))





            ;order-status (-> order-status
            order-status (-> (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries order-status)])
                             (sqlp/merge-where-false-if-empty order-status))


            requested-by-auth-user (sql/where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])


            ;ApolloError: ERROR: argument of OR must be type boolean, not type record
            ;Position: 3525
            search-term (search-query search-term)          ;; FIXME: BUG-2

            )))



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



(comment
  (let [
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds-next)


        stati '("not_processed" "in_progress" "procured" "alternative_procured" "not_procured") ;;correct

        p (println "\nquery1")
        order-status (-> (sql/select :order_status)
                         (sql/from :procurement_requests))




        advanced-user? true



        start-sqlmap (-> (request/requests-base-query-with-state advanced-user?)
                         request-helpers/join-and-nest-associated-resources)


        order-status (cond-> start-sqlmap


                             order-status (-> (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries stati)])
                                              (sqlp/merge-where-false-if-empty stati))

                             )


        p (println "\nquery3")
        query (-> order-status sql-format)


        p (println "\nquery4 formatted:" query)
        ;p (println "\nresult" (jdbc/execute! tx query))

        ]
    )
  )














(comment
  (let [
        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds-next)



        ;>oo> helper5b :order_status (not_processed in_progress procured alternative_procured not_procured)


        ;order-status (not_processed in_progress procured alternative_procured not_procured)
        stati '("not_processed" "in_progress" "procured" "alternative_procured" "not_procured") ;;correct

        p (println "\nquery1")
        order-status (-> (sql/select :*)
                         (sql/from :procurement_requests))

        ;; CORRECT SQL
        ;SELECT * FROM procurement_requests
        ;WHERE procurement_requests.order_status IN (CAST('not_processed' AS ORDER_STATUS_ENUM), CAST('in_progress' AS ORDER_STATUS_ENUM));

        ;; 1) works
        ;order-status (-> order-status
        ;                 (sql/where [:in :procurement_requests.order_status [[:cast (first stati) :order_status_enum]
        ;                                                                     [:cast (second stati) :order_status_enum]]]))


        p (println "\nquery1a" (create-order-status-enum-entries stati))

        ;; 2) works
        ;order-status (-> order-status
        ;                 (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries stati)]))

        ;; 3) works, WITH STATI
        order-status (-> order-status
                         (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries stati)])
                         (sqlp/merge-where-false-if-empty stati))



        ;; 4) works, EMPTY LIST
        ;order-status (-> order-status
        ;                 (sql/where [:in :procurement_requests.order_status (create-order-status-enum-entries stati)])
        ;                 (sqlp/merge-where-false-if-empty ()))

        p (println "\nquery3")
        query (-> order-status sql-format)


        p (println "\nquery4 formatted:" query)
        p (println "\nresult" (jdbc/execute! tx query))

        ]
    )
  )



(defn get-requests
  [context arguments value]

  (println ">debug> 50")

  (println ">oo> get-requests" value)


  (let [ring-request (:request context)
        tx (:tx-next ring-request)
        auth-entity (:authenticated-entity ring-request)
        query (as-> context <>
                (requests-query-map <> arguments value)
                (requests-perms/apply-scope tx <> auth-entity)
                (sql-format <>))


        proc-requests (request/query-requests tx auth-entity query)] ;;ERROR
       (println ">o> >>> tocheck proc-requests" query)
    (spy (->>
      (spy proc-requests)
      (map (fn [proc-req]
             (as-> proc-req <>
               (request-perms/apply-permissions tx
                                                auth-entity
                                                <>
                                                #(assoc % :request-id (:id <>)))
               (request-perms/add-action-permissions <>)))))

         )))



(defn get-total-price-cents
  [tx sqlmap]

  (println ">oo> get-total-price-cents" sqlmap)


  (or (spy(some->> sqlmap
        sql-format
        (jdbc/execute-one! tx)
        :result))
      0))

(defn- sql-sum
  [qty-type]

  (println ">oo> sql-sum" qty-type)


  (spy (as-> qty-type <>
         [:* :procurement_requests.price_cents <>]
         [:cast <> :bigint]
         [:sum <>])
  ))


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


    (-> [[:coalesce                                    ;;FIXME TODO PRIO!!!
          :procurement_requests.order_quantity
          :procurement_requests.approved_quantity]]
        (total-price-sqlmap bp-id)
        ;(total-price-sqlmap [:cast bp-id :uuid])
        (sql/where [:!= :procurement_requests.approved_quantity nil])
        (->> (get-total-price-cents tx)))

    ))
