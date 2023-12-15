(ns leihs.procurement.resources.request
  (:require (clojure [set :refer [map-invert]]
                     [string :refer [lower-case upper-case]])

            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]

            [leihs.procurement.utils.helpers :refer [my-cast]]

            ;[clojure.java.jdbc :as jdbco]

            [leihs.procurement.authorization :as authorization]
            (leihs.procurement.permissions [request-fields :as request-fields-perms]
                                           [request-helpers :as request-perms] [user :as user-perms])
            (leihs.procurement.resources [attachments :as attachments]
                                         [budget-period :as budget-period] [request-helpers :as request-helpers]
                                         [requesters-organizations :as requesters] [template :as template]
                                         [uploads :as uploads])
            (leihs.procurement.utils [helpers :refer [reject-keys submap?]])


            [next.jdbc :as jdbc]


            [taoensso.timbre :refer [debug error info spy warn]]))

(def attrs-mapping
  {:budget_period :budget_period_id,
   :category :category_id,
   :model :model_id,
   :organization :organization_id,
   :room :room_id,                                          ;; should be uuid
   :supplier :supplier_id,
   :template :template_id,
   :user :user_id})



(defn exchange-attrs
  ([req]
   (println ">debug> 5")
   (spy (exchange-attrs req attrs-mapping)))

  ([req mapping]
   (println ">debug> 6")
   (reduce (fn [mem [attr1 attr2]]
             (let [value (attr1 mem)]
               (if (contains? mem attr1)
                 (-> mem
                     (assoc attr2 value)
                     (dissoc attr1))
                 mem)))
           req
           mapping)
   ))

(defn reverse-exchange-attrs
  [req]
  (println ">debug> 4")
  (exchange-attrs req (map-invert attrs-mapping)))

(defn submap-with-id-for-associated-resources
  [m]
  (println ">debug> 3")
  (->> m
       (map (fn [[k v]]
              (if (some #{k} (keys attrs-mapping)) [k {:id v}] [k v])))
       (into {})))

(defn states-conds-map
  [advanced-user?]
  (let [approved-not-set [:= :procurement_requests.approved_quantity nil]
        approved-greater-equal-than-requested
        [:>= :procurement_requests.approved_quantity
         :procurement_requests.requested_quantity]
        approved-smaller-than-requested
        ; but greater than zero
        [:and
         [:< :procurement_requests.approved_quantity
          :procurement_requests.requested_quantity]
         [:> :procurement_requests.approved_quantity 0]]
        approved-zero [:= :procurement_requests.approved_quantity 0]]
    (cond->
      {:NEW (if advanced-user?
              approved-not-set
              [:or
               [:and [:< :procurement_budget_periods.end_date :current_date]
                approved-not-set]
               [:< :current_date
                :procurement_budget_periods.inspection_start_date]]),
       :APPROVED (if advanced-user?
                   approved-greater-equal-than-requested
                   [:and [:< :procurement_budget_periods.end_date :current_date]
                    approved-greater-equal-than-requested]),
       :PARTIALLY_APPROVED (if advanced-user?
                             approved-smaller-than-requested
                             [:and
                              [:< :procurement_budget_periods.end_date
                               :current_date] approved-smaller-than-requested]),
       :DENIED (if advanced-user?
                 approved-zero
                 [:and [:< :procurement_budget_periods.end_date :current_date]
                  approved-zero])}
      (not advanced-user?)
      (assoc :IN_APPROVAL
             [:and
              [:>= :current_date :procurement_budget_periods.inspection_start_date]
              [:< :current_date :procurement_budget_periods.end_date]]))))


(comment
  (let [
        res (states-conds-map true)
        p (println ">1>" res)

        res (states-conds-map false)
        p (println ">1>" res)

        ])

  )


(defn get-where-conds-for-states
  [states advanced-user?]
  (println ">debug> 2")
  (reduce (fn [or-conds state]
            (let [sc-map (states-conds-map advanced-user?)]
              (->> sc-map
                   state
                   (conj or-conds))))
          [:or]
          states))

(defn to-name-and-lower-case
  [x]
  (println ">debug> 1a")
  (-> x
      name
      lower-case))

(defn to-name-and-upper-case
  [x]
  (println ">debug> 7")
  (-> x
      name
      upper-case))

(defn debug-print [item]
  (println ">o> map" item)
  item)


(defn state-sql
  [advanced-user?]

  (println ">debug> 8c")
  (println ">o>" "state-sql" advanced-user?)

  (let [s-map (states-conds-map advanced-user?)
        p (println ">o> s-map " s-map)
        ;p (println ">o> s-map " s-map)
        ]                                                   ;;FIXME

    ;{:NEW [:= :procurement_requests.approved_quantity nil], :APPROVED [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity], :PARTIALLY_APPROVED [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]], :DENIED [:= :procurement_requests.approved_quantity 0]}

    (let [

          map (->> s-map
                   keys
                   (map name)
                   (map debug-print)                        ; Added print function for each key
                   (interleave (vals s-map))
                   (map debug-print)                        ; Added print function for each value
                   (cons :case))

          p (println ">o>map-4" map)                        ;;TODO: whats this, FIXME NOW

          result [[map]]
          p (println ">o>map-5" map)                        ;;TODO: whats this, FIXME NOW

          ;>o>map-4 (:call :case [:= :procurement_requests.approved_quantity nil] NEW [:>= :procurement_requests.approved_quantity :procurement_requests.requested_quantity] APPROVED [:and [:< :procurement_requests.approved_quantity :procurement_requests.requested_quantity] [:> :procurement_requests.approved_quantity 0]] PARTIALLY_APPROVED [:= :procurement_requests.approved_quantity 0] DENIED)

          ] (spy result))

    ;(spy (->> s-map
    ;          keys
    ;          (map name)
    ;          (map debug-print)                             ; Added print function for each key
    ;          (interleave (vals s-map))
    ;          (map debug-print)                             ; Added print function for each value
    ;          (cons :case)))

    ))

(def sql-order-by-expr                                      ;;toRemove
  (str "concat("
       "lower(coalesce(procurement_requests.article_name, '')), "
       "lower(coalesce(models.product, '')), "
       "lower(coalesce(models.version, ''))" ")"))

(def requests-base-query                                    ;;ok
  (let [
        conc sql-order-by-expr

        p (println ">o> conc" conc)
        ]

    (spy (-> (sql/select
               [[:raw (str "DISTINCT ON (procurement_requests.id, "
                           conc
                           ") procurement_requests.*")]]
               )

             (sql/from :procurement_requests)
             (sql/left-join :models [:= :models.id :procurement_requests.model_id])
             (sql/order-by :procurement_requests.id [[:raw conc]]) ;; master-version
             ))
    ))


(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2" ;; >>3 []
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 []
        ;user-id nil ;; >>3 []
        ;auth-entity {:user_id user-id}
        ;
        ;c-id nil
        ;c-id #uuid "1efc2279-bc42-490c-b004-dca03813a6ef"
        ;
        ;tx (db/get-ds-next)


        conc [:concat
              [:lower [:coalesce :procurement_requests/article_name ""]]
              [:lower [:coalesce :models/product ""]]
              [:lower [:coalesce :models/version ""]]]

        conc (sql-format conc)

        p (println ">o> conc" conc)
        ;p (println ">o> conc" (first conc))

        ]
    )

  )


(comment

  ;[honey.sql :refer [format] :rename {format sql-format}]
  ;[leihs.core.db :as db]
  ;[next.jdbc :as jdbc]
  ;[honey.sql.helpers :as sql]

  (let [
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 [{:has_entry true}]
        ;user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3b2" ;; >>3 []
        user-id #uuid "3eaba478-f710-4cb8-bc87-54921a27e3bb" ;; >>3 []
        user-id nil                                         ;; >>3 []
        auth-entity {:user_id user-id}

        c-id nil
        c-id #uuid "1efc2279-bc42-490c-b004-dca03813a6ef"

        advanced-user? true

        tx (db/get-ds-next)

        ;;; TODO: this works
        ;res (-> (sql/select [[:raw (str "DISTINCT ON (procurement_requests.id, "
        ;                                sql-order-by-expr
        ;                                ") procurement_requests.*")]])
        ;        (sql/from :procurement_requests)
        ;        (sql/left-join :models [:= :models.id :procurement_requests.model_id]))


        ;(def sql-order-by-expr
        ;  (str "concat("
        ;       "lower(coalesce(procurement_requests.article_name, '')), "
        ;       "lower(coalesce(models.product, '')), "
        ;       "lower(coalesce(models.version, ''))" ")"))

        ;conc [:concat [:lower [:coalesce :procurement_requests.article_name ""]]]

        ;conc [:concat
        ;      [:lower [:coalesce :procurement_requests.article_name ""]]
        ;      [:lower [:coalesce :models.product ""]]
        ;      [:lower [:coalesce :models.version ""]]
        ;      ]

        ;; works
        conc [[:concat
               [:lower [:coalesce :procurement_requests/article_name ""]]
               [:lower [:coalesce :models/product ""]]
               [:lower [:coalesce :models/version ""]]
               ]]


        ;p (println "\n>o>1 conc" conc)
        ;p (println "\n>o>2 conc" (sql-format conc))
        ;conc2 (sql-format conc)


        res2 (-> (sql/select conc)
                 (sql/from :procurement_requests :models)
                 )

        p (println "\n>o>3 query" (sql-format res2))
        p (println "\n>o>4 result" (jdbc/execute! tx (sql-format res2)))




        ;; TODO: this works
        ;;res (-> (sql/select [[:raw (str "DISTINCT ON (procurement_requests.id, "
        ;;res (-> (sql/select-distinct :procurement_requests.*)
        ;res (-> (sql/select-distinct :procurement_requests.id conc :procurement_requests.*)
        res (spy (-> (sql/select-distinct :procurement_requests.id conc)
                     (sql/from :procurement_requests)
                     (sql/left-join :models [:= :models.id :procurement_requests.model_id])
                     ;(sql/order-by :procurement_requests.id conc :procurement_requests.*)
                     (sql/order-by :procurement_requests.id conc)
                     ))

        ;>o> query [SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.* FROM procurement_requests LEFT JOIN models ON models.id = procurement_requests.model_id]



        p (println "\n>o>3 query" (spy (sql-format res)))
        ;p (Execution error (PSQLException) at org.postgresql.core.v3.QueryExecutorImpl/receiveErrorResponse (QueryExecutorImpl.java:2533).
        ;             ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list
        ;             Position: 305
        ;             println "\n>o>4 result" (jdbc/execute! tx (sql-format res)))

        ]
    )

  )


(defn requests-base-query-with-state
  [advanced-user?]
  (println ">debug> 9b")
  (println ">o>" "requests-base-query-with-state")
  (-> requests-base-query
      (sql/select [(state-sql advanced-user?) :state])
      (sql/join :procurement_budget_periods
                [:= :procurement_budget_periods.id
                 :procurement_requests.budget_period_id]))
  )

(defn to-name-and-lower-case-enums
  [m]
  (println ">debug> 10")
  (println ">oo> >here> to-name-and-lower-case-enums" m)

  (cond-> m
          (:order_status m) (update :order_status to-name-and-lower-case)
          (:priority m) (update :priority to-name-and-lower-case)
          (:inspector_priority m) (update :inspector_priority to-name-and-lower-case)))

(defn upper-case-keyword-value
  [row attr]
  (println ">debug> 11")
  (println ">o> upper-case-keyword-value: row =>" row)
  (println ">o> upper-case-keyword-value: attr =>" attr)

  (update row
          attr
          #(-> %
               upper-case
               keyword)))

(defn treat-order-status [row]
  (println ">debug> 12")
  (println ">o> treat-order-status: HERE row =>" row)
  (let [
        result (upper-case-keyword-value row :order_status)
        p (println ">o> treat-:order_status: upperCase =>" (:order_status result))
        p (if (nil? (:order_status row))
            (throw (Exception. "treat-order-status _> nill")))
        ] result)
  )

(defn treat-priority [row]
  (println ">debug> 13")
  (println ">o> treat-priority: row =>" row)
  (let [
        result (upper-case-keyword-value row :priority)
        p (println ">o> treat-priority: upperCase =>" (:priority result))
        p (if (nil? (:priority row))
            (throw (Exception. "treat-priority _> nill")))
        ] result)
  )

(defn treat-inspector-priority
  [row]
  (println ">debug> 14")
  (println ">o> treat-inspector-priority: row =>" row)
  (let [
        result (upper-case-keyword-value row :inspector_priority)
        p (println ">o> treat-priority: upperCase =>" (:inspector_priority result))
        p (if (nil? (:inspector_priority row))
            (throw (Exception. "treat-inspector-priority _> nill")))

        ] result)
  )

(defn initialize-attachments-attribute
  [row]
  (println ">debug> 15")
  (assoc row :attachments :unqueried))

(defn add-total-price
  [row advanced-user?]
  (println ">debug> 16")
  (let [transparent-quantity (or (:order_quantity row)
                                 (:approved_quantity row)
                                 (:requested_quantity row))
        quantity
        (if (-> row
                :budget_period
                :is_past)
          transparent-quantity
          (if advanced-user? transparent-quantity (:requested_quantity row)))]
    (->> row
         :price_cents
         (* quantity)
         (assoc row :total_price_cents))))

(defn enum-state
  [row]
  (println ">debug> 17")

  (println ">o> treat-inspector-priority: enum-state =>" row)

  (let [

        p (println ">o> upper-case-keyword-value: attr / :state =>" row)
        result (->> row
                    :state
                    keyword
                    (assoc row :state))
        p (println ">o> treat-inspector-priority: enum-state _> " (:state result))
        ]

    result)

  )

(defn add-general-ledger-account
  [row]
  (println ">debug> 18")
  (->> row
       :category
       :general_ledger_account
       (assoc row :general_ledger_account)))

(defn add-cost-center
  [row]
  (println ">debug> 19")
  (->> row
       :category
       :cost_center
       (assoc row :cost_center)))

(defn add-procurement-account
  [row]
  (println ">debug> 20")
  (->> row
       :category
       :procurement_account
       (assoc row :procurement_account)))

(defn dissoc-foreign-keys [row]
  (println ">debug> 21")
  (apply dissoc row (vals attrs-mapping)))

(defn transform-row
  [row advanced-user?]
  (println ">debug> 22")

  (println ">oo> treat-order-status HERE: transform-row" row advanced-user?)

  (-> row
      enum-state
      add-general-ledger-account
      add-cost-center
      add-procurement-account
      (add-total-price advanced-user?)
      treat-order-status
      treat-priority
      treat-inspector-priority
      initialize-attachments-attribute
      dissoc-foreign-keys))

;(defn query-requests
;  [tx auth-entity query]
;
;  (println ">o> query-requests, auth-entity" auth-entity)
;  (println ">o> query-requests, query" query)
;  (let [advanced-user? (user-perms/advanced? tx auth-entity)
;
;        p (println ">o> query-requests::advanced-user?" advanced-user?)
;        p (println ">o> query-requests::query" query)
;
;        ;result (jdbc/execute! tx query)                     ;;ERROR
;        p (println ">o> 1query-requests::result" result)
;
;        ;p (println ">o> 1aquery-requests::fnc-blabla" {:row-fn #(transform-row % advanced-user?)})
;
;        result (jdbco/query tx query {:row-fn #(transform-row % advanced-user?)}) ;TODO: BUG-1 activate this
;        ;p (println ">o> 2query-requests::result" result)
;
;        ]
;
;    result
;
;    ;; TODO, search contains a weired WHERE TRUE=FALSE query
;    ;(jdbc/execute! tx query {:row-fn #(transform-row % advanced-user?)})
;    ))

(defn query-requests
  [tx auth-entity query]

  (println ">debug> 23d")

  (println ">o> HERE query-requests, auth-entity" auth-entity)
  (println ">o> query-requests, query" query)

  (let [advanced-user? (user-perms/advanced? tx auth-entity)

        ;; TODO: FIXME, :row-fn doesnt work in new jdbc-version
        ;result (jdbc/execute! tx query {:row-fn #(transform-row % advanced-user?)}) ;; :row-fn

        result (->> (jdbc/execute! tx (spy query))
                    (map #(transform-row % advanced-user?)))

        p (println ">o> >o> HERE :row-fn" result)
        ]

    ;(throw (Exception. "fake error"))

    (spy result)))



(comment

  ;; order_status should be uppercased:
  (let [
        req_id #uuid "fad5c7f6-4943-53b8-9fa6-9a533dc938ff"
        tx (db/get-ds-next)
        advanced-user? true

        query (-> (sql/select :*)
                  (sql/from :procurement_requests)
                  (sql/where [:= :id [:cast req_id :uuid]])
                  (sql/limit 1)
                  sql-format
                  )
        ;query (sql/format {:select :*
        ;                   :from :procurement_requests
        ;                   :where [:= :id [:cast req_id :uuid]]})

        p (println "\nquery" query)

        result (jdbc/execute! tx query)
        ;p (println "\nresult-1" result)
        p (println "\nresult-1" (:order_status (first result)))


        ;; FIXME: THIS DOESNT WORK
        ;result (jdbc/execute! tx query {:builder-fn #(transform-row % advanced-user?)}) ;;broken

        p (println "\nresult-2a")
        ;result (jdbc/execute! tx query {:builder-fn (custom-row-builder advanced-user?)})
        ;result (jdbc/execute! tx query {:builder-fn (make-custom-row-builder advanced-user?)})
        ;result (jdbc/execute! tx query {:builder-fn (custom-row-builder advanced-user?)})

        result (->> (jdbc/execute! tx query)
                    (map #(transform-row % advanced-user?)))



        p (println "\nresult-2b" result)
        ;p (println "\nresult-2" (:order_status (first result)))
        ]
    result
    )
  )

(defn get-request-by-id-sqlmap
  [tx auth-entity id]
  (println ">debug> 24")

  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        (sql/where [:= :procurement_requests.id [:cast id :uuid]]))))

(defn get-request-by-id
  [tx auth-entity id]
  (println ">debug> 25")

  (->> id
       (get-request-by-id-sqlmap tx auth-entity)
       request-helpers/join-and-nest-associated-resources
       sql-format
       (query-requests tx auth-entity)
       first))

(defn- consider-default
  [attr p-spec]
  (println ">debug> 26")

  (if (:value p-spec)
    {attr p-spec}
    (->> p-spec
         :default
         (if (:read p-spec))
         (assoc p-spec :value)
         (hash-map attr))))

(defn get-new
  [context args value]
  (println ">debug> 27 get-new")

  (let [ring-req (:request context)
        tx (:tx-next ring-req)
        auth-entity (:authenticated-entity ring-req)
        user-arg (:user args)
        req-stub (cond-> args
                         (not user-arg) (assoc :user (:user_id auth-entity)))
        fields
        (->> req-stub
             submap-with-id-for-associated-resources
             (request-fields-perms/get-for-user-and-request tx auth-entity))]
    (authorization/authorize-and-apply
      #(as-> fields <>
         (reject-keys <> request-perms/special-perms)
         (map (fn [f] (apply consider-default f)) <>)
         (into {} <>)
         (assoc <> :state :NEW))
      :if-only
      #(request-perms/can-write-any-field? fields))))




(comment

  (let [

        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
        tx (db/get-ds-next)

        advanced-user? true
        advanced-user? false


        ;query2 (-> (sql/select [[(state-sql advanced-user?)] :state]) ;;works (old solution)
        ;query2 (-> (sql/select (state-sql advanced-user?) :state) ;;fails
        query2 (-> (sql/select [(state-sql advanced-user?) :state]) ;;works (new)
                   ;query2 (-> (sql/select [[[(state-sql advanced-user?)]] :state]) ;;works
                   (sql/from :procurement_requests :procurement_budget_periods)
                   sql-format
                   )


        p (println "\nquery2" query2)
        p (println "\nquery3" (jdbc/execute! tx query2))
        ]

    )
  )




(defn get-last-created-request
  [tx auth-entity]
  (println ">debug> 28 >>> here tocheck ???")

  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (spy (-> advanced-user?
             requests-base-query-with-state
             ; NOTE: reselect because of:
             ; ERROR: SELECT DISTINCT ON expressions must match initial ORDER BY expressions

             (sql/select :procurement_requests.* [(state-sql advanced-user?) :state]) ;maybe fixed?? (new, like master)

             ;(sql/select :procurement_requests.* [state-sql advanced-user?) :state]) ; original

             (sql/order-by [:created_at :desc])
             (sql/limit 1)
             sql-format
             spy
             (->> (query-requests tx auth-entity))
             first))))




;(defn my-cast [data]
;  (println ">o> no / 22 / my-cast /debug " data)
;
;
;  (let [
;        data (if (contains? data :id)
;               (assoc data :id [[:cast (:id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :category_id)
;               (assoc data :category_id [[:cast (:category_id data) :uuid]])
;               data
;               )
;        data (if (contains? data :template_id)
;               (assoc data :template_id [[:cast (:template_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :room_id)
;               (assoc data :room_id [[:cast (:room_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :order_status)
;               (assoc data :order_status [[:cast (:order_status data) :order_status_enum]])
;               data
;               )
;
;        data (if (contains? data :budget_period_id)
;               (assoc data :budget_period_id [[:cast (:budget_period_id data) :uuid]])
;               data
;               )
;
;        data (if (contains? data :user_id)
;               (assoc data :user_id [[:cast (:user_id data) :uuid]])
;               data
;               )
;
;        ;[[:cast (to-name-and-lower-case a) :order_status_enum]]
;
;        ]
;    (spy data)
;    )
;
;  )


(defn insert!
  [tx data]
  (println ">debug> 29")


  (let [

        data (spy (my-cast data))

        result (jdbc/execute! tx
                              (-> (sql/insert-into :procurement_requests)
                                  (sql/values [data])
                                  sql-format
                                  spy
                                  ))
        ]

    (spy result)
    )
  )


;
;(defn my-cast [data]
;  (println ">o> no / my-cast /debug " data)
;  (if (contains? data :room_id)
;    (let [
;          p (println ">o> no before _> room_id=" (:room_id data))
;          ;(assoc data :room_id (java.util.UUID/fromString (:room_id data)))
;          ;(assoc data :room_id [:cast (:room_id data) :uuid])
;          data (assoc data :room_id [[:cast (:room_id data) :uuid]])
;          p (println ">o> no after _> room_id=" data)
;          ] data)
;    data
;    )
;  )


(defn update!
  [tx req-id data]
  (println ">debug> 30")
  (println ">debug> update!=" data)


  (println ">o> no before quest _> room_id=" (my-cast data))


  (jdbc/execute! tx
                 (-> (sql/update :procurement_requests)     ;;fixme
                     (sql/set (my-cast data))
                     (sql/where [:= :procurement_requests.id [:cast req-id :uuid]])
                     ;(sql/where :raw "/* da sama 123 */" )
                     ;(sql/where [:= :procurement_requests.id [:cast req-id :uuid]])
                     sql-format
                     spy
                     )))

(defn- filter-attachments [m as]

  (println ">debug> 31")

  (filter #(submap? m %) as))

(defn deal-with-attachments!
  [tx req-id attachments]
  (println ">debug> 32")

  (let [uploads-to-delete
        (filter-attachments {:to_delete true, :typename "Upload"} attachments)
        uploads-to-attachments (filter-attachments {:to_delete false,
                                                    :typename "Upload"}
                                                   attachments)
        attachments-to-delete (filter-attachments {:to_delete true,
                                                   :typename "Attachment"}
                                                  attachments)
        ; NOTE: just for purpose of completeness and clarity:
        ; don't do anything with existing attachments
        ; attachments-to-retain
        ; (filter-attachments {:to_delete false, :typename "Attachment"}
        ; attachments)
        ]
    (if-not (empty? uploads-to-delete)
      (uploads/delete! tx (map :id uploads-to-delete)))
    (if-not (empty? attachments-to-delete)
      (attachments/delete! tx (map :id attachments-to-delete)))
    (if-not (empty? uploads-to-attachments)
      (attachments/create-for-request-id-and-uploads! tx
                                                      req-id
                                                      uploads-to-attachments))))

(defn change-budget-period!
  [context args _]

  (println ">debug> 33")
  (let [ring-req (:request context)
        tx (:tx-next ring-req)
        auth-entity (:authenticated-entity ring-req)
        input-data (:input_data args)
        req-id (:id input-data)
        new-budget-period-id (:budget_period input-data)
        budget-period-new
        (budget-period/get-budget-period-by-id tx new-budget-period-id)
        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      #(jdbc/execute! tx
                      (-> (sql/update :procurement_requests)
                          (sql/set (my-cast {:budget_period_id new-budget-period-id}))
                          (sql/where [:= :procurement_requests.id [:cast req-id :uuid]])
                          sql-format))
      :if-only
      #(and (not (budget-period/past? tx budget-period-new))
            (request-perms/authorized-to-write-all-fields?
              tx
              auth-entity
              proc-request
              {:budget_period {:id new-budget-period-id}})))
    (->> req-id
         (get-request-by-id tx auth-entity)
         (request-perms/apply-permissions tx auth-entity))))

(def change-category-reset-attrs
  {:approved_quantity nil,
   :inspection_comment nil,
   :inspector_priority "medium",
   :order_quantity nil})

(defn change-category!
  [context args _]
  (println ">debug> 34")

  (let [ring-req (:request context)
        tx (:tx-next ring-req)
        auth-entity (:authenticated-entity ring-req)
        input-data (:input_data args)
        req-id (:id input-data)
        cat-id (:category input-data)
        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      #(jdbc/execute!
         tx
         (-> (sql/update :procurement_requests)
             (sql/set
               (cond-> {:category_id [:cast cat-id :uuid]}
                       (and (not (user-perms/inspector? tx auth-entity cat-id))
                            (not (user-perms/admin? tx auth-entity)))
                       (merge change-category-reset-attrs)))
             (sql/where [:= :procurement_requests.id [:cast req-id :uuid]])
             sql-format))
      :if-only
      #(request-perms/authorized-to-write-all-fields? tx
                                                      auth-entity
                                                      proc-request
                                                      {:category {:id cat-id}}))
    (->> req-id
         (get-request-by-id tx auth-entity)
         (request-perms/apply-permissions tx auth-entity))))

(defn create-request!
  [context args _]

  (println ">debug> 35")

  (let [ring-req (:request context)
        tx (:tx-next ring-req)
        auth-entity (:authenticated-entity ring-req)
        input-data (:input_data args)
        attachments (:attachments input-data)
        template (if-let [t-id (:template input-data)]
                   (template/get-template-by-id tx t-id))
        data-from-template (-> template
                               (dissoc :id))
        user-id (or (:user input-data) (:user_id auth-entity))
        organization (requesters/get-organization-of-requester tx user-id)
        write-data (-> input-data
                       (dissoc :attachments)
                       (assoc :user user-id)
                       (assoc :organization (:id organization))
                       to-name-and-lower-case-enums)]
    (let [req-id (atom nil)]
      (authorization/authorize-and-apply
        #(do (insert! tx
                      (-> write-data
                          (cond-> template (merge (dissoc data-from-template :is_archived)))
                          exchange-attrs))
             (reset! req-id
                     (-> (get-last-created-request tx auth-entity)
                         :id))
             (if-not (empty? attachments)
               (deal-with-attachments! tx @req-id attachments)))
        :if-all
        [#(->> input-data
               submap-with-id-for-associated-resources
               (request-perms/authorized-to-write-all-fields? tx auth-entity))
         #(or (not template) (not (:is_archived template)))])
      (as-> @req-id <>
        (get-request-by-id tx auth-entity <>)
        (request-perms/apply-permissions tx
                                         auth-entity
                                         <>
                                         #(assoc %
                                            :request-id @req-id))))))


(defn cast-to-order-status-enum [a]
  (println ">debug> 36")

  (println ">oo> >here> cast-to-order-status-enum" a)
  [[:cast (to-name-and-lower-case a) :order_status_enum]]
  )


;(comment                                                    ;; do sama
;  (let [
;        user-id #uuid "37bb3d3d-3a61-4f98-863e-c549568317f0"
;        tx (db/get-ds)
;
;        raw-order-status '[NOT_PROCESSED IN_PROGRESS PROCURED ALTERNATIVE_PROCURED NOT_PROCURED]
;        p (println ">o> raw-order-status" raw-order-status)
;
;        order-status (some->> raw-order-status
;                       (map request/to-name-and-lower-case))
;        p (println ">o> order-status" order-status)
;
;        os-map (create-order-status-enum-entries order-status)
;        p (println ">o> order-os-map" os-map)
;
;        sql (-> (sql/select :*)
;                (sql/from :procurement_requests)
;                (sql/where [:in :procurement_requests.order_status
;                            os-map
;
;                            ;[[ [:cast (first order-status) :order_status_enum]]] ;;works, 1 entry
;                            ;[[ [:cast (second order-status) :order_status_enum]]] ;;works, no entry
;
;                            ])
;                )
;
;        p (println "\nsql" sql)
;        query (sql-format sql)
;
;        p (println "\nquery" query)
;        p (println "\nresult" (jdbc/execute! tx query))]
;    )
;  )

(defn update-request!
  [context args _]

  (println ">debug> 37")

  (let [ring-req (:request context)
        tx (:tx-next ring-req)
        auth-entity (:authenticated-entity ring-req)
        input-data (:input_data args)
        req-id (:id input-data)
        attachments (:attachments input-data)
        organization-id (some->> input-data
                          :user
                          (requesters/get-organization-of-requester tx)
                          :id)

        p (println ">oo> input-data >here> " input-data)
        p (println ">oo> input-data >here> " (:order_status input-data))

        update-data (as-> input-data <>
                      (dissoc <> :id)
                      (dissoc <> :attachments)
                      (cond-> <>
                              (:order_status <>)
                              (update :order_status cast-to-order-status-enum)) ;;TODO
                      ;(update :order_status
                      ;        #(:call :cast (to-name-and-lower-case %) :order_status_enum)))

                      (cond-> <> (:priority <>) (update :priority to-name-and-lower-case))
                      (cond-> <> (:inspector_priority <>) (update :inspector_priority to-name-and-lower-case))
                      (cond-> <> organization-id (assoc :organization_id organization-id)))

        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      ;[leihs.procurement.resources.request:972] - (update! tx req-id (exchange-attrs update-data)) => [{:next.jdbc/update-count 1}]
      #(do (spy (update! tx req-id (exchange-attrs update-data)))
           (if-not (empty? attachments)
             (deal-with-attachments! tx req-id attachments)))
      :if-only
      #(request-perms/authorized-to-write-all-fields?
         tx
         auth-entity
         proc-request
         (-> input-data
             (reject-keys request-perms/attrs-to-skip)
             submap-with-id-for-associated-resources)))
    (as-> req-id <>
      (get-request-by-id tx auth-entity <>)
      (request-perms/apply-permissions tx
                                       auth-entity
                                       <>
                                       #(assoc % :request-id [:cast req-id :uuid])))))




(comment
  (let [
        input-data {
                    :order_status "NOT_PROCURED"
                    :attachment [{:name "example1"} {:name "example2"}]
                    :id "my-id-123"
                    }

        ;; expected result (old version)
        ;{:order_status #sql/call [:cast not_procured :order_status_enum], :attachment [{:name example1} {:name example2}]}

        ;; new result
        ;{:order_status [[:cast not_procured :order_status_enum]], :attachment [{:name example1} {:name example2}]}

        result (as-> input-data <>
                 (dissoc <> :id)
                 (dissoc <> :attachments)
                 (cond-> <> (:order_status <>)
                         (update :order_status cast-to-order-status-enum)
                         ;#(sql/call :cast (to-name-and-lower-case %) :order_status_enum) ;works, old version
                         ))

        p (println ">o> result" result)

        ])
  )



(defn delete-request!
  [context args _]

  (println ">debug> 38")

  (let [ring-request (:request context)
        tx (:tx-next ring-request)
        auth-entity (:authenticated-entity ring-request)
        req-id (-> args
                   :input_data
                   :id)
        request (get-request-by-id tx auth-entity req-id)
        field-perms (request-fields-perms/get-for-user-and-request tx
                                                                   auth-entity
                                                                   request)]
    (authorization/authorize-and-apply
      #(let [result (jdbc/execute-one! tx (-> (sql/delete-from :procurement_requests)
                                          (sql/where [:= :procurement_requests.id [:cast req-id :uuid]])
                                          sql-format) {:builder-fn next.jdbc.result-set/as-unqualified-maps})

             ;; TODO: FIXME, namespace shouldn't be needed here
             result-count (spy (:update-count (spy result)))                ;; fails
             result-count (spy (:next.jdbc/update-count (spy result)))      ;; works
             ]

         (spy (= (spy result-count) 1))
         )
      :if-only
      #(:DELETE field-perms))))

(defn requested-by?
  [tx auth-entity request]

  (println ">debug> 39")
  (println ">debug> 39 request=" request)
  (println ">debug> 39 reques.idt=" (:id request))

  (= (:user_id auth-entity)
     (-> requests-base-query
         (sql/where [:= :procurement_requests.id [:cast (:id request) :uuid]])
         sql-format
         (->> (query-requests tx auth-entity))
         first
         :user_id)))
