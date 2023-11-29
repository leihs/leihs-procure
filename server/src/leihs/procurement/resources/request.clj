(ns leihs.procurement.resources.request
  (:require (clojure [set :refer [map-invert]]
                     [string :refer [lower-case upper-case]])

            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.core.db :as db]

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
   :room :room_id,
   :supplier :supplier_id,
   :template :template_id,
   :user :user_id})

(defn exchange-attrs
  ([req] (exchange-attrs req attrs-mapping))
  ([req mapping]
   (reduce (fn [mem [attr1 attr2]]
             (let [value (attr1 mem)]
               (if (contains? mem attr1)
                 (-> mem
                     (assoc attr2 value)
                     (dissoc attr1))
                 mem)))
           req
           mapping)))

(defn reverse-exchange-attrs
  [req]
  (exchange-attrs req (map-invert attrs-mapping)))

(defn submap-with-id-for-associated-resources
  [m]
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
  (reduce (fn [or-conds state]
            (let [sc-map (states-conds-map advanced-user?)]
              (->> sc-map
                   state
                   (conj or-conds))))
          [:or]
          states))

(defn to-name-and-lower-case
  [x]
  (-> x
      name
      lower-case))

(defn debug-print [item]
  (println ">o> map" item)
  item)


(defn state-sql
  [advanced-user?]

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
  ;(-> (sql/select [[:raw (str "DISTINCT ON (procurement_requests.id, "
  ;                            sql-order-by-expr
  ;                            ") procurement_requests.*")]])
  ;    (sql/from :procurement_requests)
  ;    (sql/left-join :models [:= :models.id :procurement_requests.model_id])
  ;    (sql/order-by (:raw sql-order-by-expr)))

  (let [

        conc [[:concat
               [:lower [:coalesce :procurement_requests/article_name ""]]
               [:lower [:coalesce :models/product ""]]
               [:lower [:coalesce :models/version ""]]
               ]]
        conc (sql-format conc)
        conc (first conc)
        p (println ">o> conc" conc)
        ]

    ;(-> (sql/select-distinct-on [:procurement_requests.id conc :procurement_requests.*]) ;; FIXME / broken
    (-> (sql/select [[:raw (str "DISTINCT ON (procurement_requests.id, "
                                conc
                                ") procurement_requests.*")]])



        (sql/from :procurement_requests)
        (sql/left-join :models [:= :models.id :procurement_requests.model_id])
        (sql/order-by :procurement_requests.id conc :procurement_requests.*)
        )
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

        p (println ">o> conc" (first conc))

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
        res (-> (sql/select-distinct :procurement_requests.id conc)
                (sql/from :procurement_requests)
                (sql/left-join :models [:= :models.id :procurement_requests.model_id])
                (sql/order-by :procurement_requests.id conc :procurement_requests.*)
                )

        ;>o> query [SELECT DISTINCT ON (procurement_requests.id, concat(lower(coalesce(procurement_requests.article_name, '')), lower(coalesce(models.product, '')), lower(coalesce(models.version, '')))) procurement_requests.* FROM procurement_requests LEFT JOIN models ON models.id = procurement_requests.model_id]



        p (println "\n>o>3 query" (sql-format res))
        ;p (Execution error (PSQLException) at org.postgresql.core.v3.QueryExecutorImpl/receiveErrorResponse (QueryExecutorImpl.java:2533).
        ;             ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list
        ;             Position: 305
        ;             println "\n>o>4 result" (jdbc/execute! tx (sql-format res)))

        ]
    )

  )


(defn requests-base-query-with-state
  [advanced-user?]
  (println ">o>" "requests-base-query-with-state")
  (-> requests-base-query
      (sql/select [(state-sql advanced-user?) :state])
      (sql/join :procurement_budget_periods
                [:= :procurement_budget_periods.id
                 :procurement_requests.budget_period_id]))

  )

(defn to-name-and-lower-case-enums
  [m]
  (cond-> m
          (:order_status m) (update :order_status to-name-and-lower-case)
          (:priority m) (update :priority to-name-and-lower-case)
          (:inspector_priority m) (update :inspector_priority
                                          to-name-and-lower-case)))

(defn upper-case-keyword-value
  [row attr]
  (update row
          attr
          #(-> %
               upper-case
               keyword)))

(defn treat-order-status [row] (upper-case-keyword-value row :order_status))

(defn treat-priority [row] (upper-case-keyword-value row :priority))

(defn treat-inspector-priority
  [row]
  (upper-case-keyword-value row :inspector_priority))

(defn initialize-attachments-attribute
  [row]
  (assoc row :attachments :unqueried))

(defn add-total-price
  [row advanced-user?]
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
  (->> row
       :state
       keyword
       (assoc row :state)))

(defn add-general-ledger-account
  [row]
  (->> row
       :category
       :general_ledger_account
       (assoc row :general_ledger_account)))

(defn add-cost-center
  [row]
  (->> row
       :category
       :cost_center
       (assoc row :cost_center)))

(defn add-procurement-account
  [row]
  (->> row
       :category
       :procurement_account
       (assoc row :procurement_account)))

(defn dissoc-foreign-keys [row]
  (apply dissoc row (vals attrs-mapping)))

(defn transform-row
  [row advanced-user?]
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

(defn query-requests
  [tx auth-entity query]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (jdbc/execute! tx query {:row-fn #(transform-row % advanced-user?)})))

(defn get-request-by-id-sqlmap
  [tx auth-entity id]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        (sql/where [:= :procurement_requests.id id]))))

(defn get-request-by-id
  [tx auth-entity id]
  (->> id
       (get-request-by-id-sqlmap tx auth-entity)
       request-helpers/join-and-nest-associated-resources
       sql-format
       (query-requests tx auth-entity)
       first))

(defn- consider-default
  [attr p-spec]
  (if (:value p-spec)
    {attr p-spec}
    (->> p-spec
         :default
         (if (:read p-spec))
         (assoc p-spec :value)
         (hash-map attr))))

(defn get-new
  [context args value]
  (let [ring-req (:request context)
        tx (:tx ring-req)
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
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        ; NOTE: reselect because of:
        ; ERROR: SELECT DISTINCT ON expressions must match initial ORDER BY expressions

        (sql/select :procurement_requests.* [(state-sql advanced-user?) :state]) ;maybe fixed?? (new, like master)

        ;(sql/select :procurement_requests.* [state-sql advanced-user?) :state]) ; original

        (sql/order-by [:created_at :desc])
        (sql/limit 1)
        sql-format
        (->> (query-requests tx auth-entity))
        first)))

(defn insert!
  [tx data]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_requests)
                     (sql/values [data])
                     sql-format)))

(defn update!
  [tx req-id data]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_requests)
                     (sql/set data)
                     (sql/where [:= :procurement_requests.id req-id])
                     sql-format)))

(defn- filter-attachments [m as] (filter #(submap? m %) as))

(defn deal-with-attachments!
  [tx req-id attachments]
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
  (let [ring-req (:request context)
        tx (:tx ring-req)
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
                          (sql/set {:budget_period_id new-budget-period-id})
                          (sql/where [:= :procurement_requests.id req-id])
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
  (let [ring-req (:request context)
        tx (:tx ring-req)
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
               (cond-> {:category_id cat-id}
                       (and (not (user-perms/inspector? tx auth-entity cat-id))
                            (not (user-perms/admin? tx auth-entity)))
                       (merge change-category-reset-attrs)))
             (sql/where [:= :procurement_requests.id req-id])
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
  (let [ring-req (:request context)
        tx (:tx ring-req)
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

(defn update-request!
  [context args _]
  (let [ring-req (:request context)
        tx (:tx ring-req)
        auth-entity (:authenticated-entity ring-req)
        input-data (:input_data args)
        req-id (:id input-data)
        attachments (:attachments input-data)
        organization-id (some->> input-data
                          :user
                          (requesters/get-organization-of-requester tx)
                          :id)
        update-data
        (as-> input-data <>
          (dissoc <> :id)
          (dissoc <> :attachments)
          (cond-> <> (:order_status <>)
                  (update :order_status
                          #(:call :cast (to-name-and-lower-case %) :order_status_enum)))
          (cond-> <> (:priority <>) (update :priority to-name-and-lower-case))
          (cond-> <>
                  (:inspector_priority <>) (update :inspector_priority
                                                   to-name-and-lower-case))
          (cond-> <>
                  organization-id (assoc :organization_id organization-id)))
        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      #(do (update! tx req-id (exchange-attrs update-data))
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
                                       #(assoc % :request-id req-id)))))

(defn delete-request!
  [context args _]
  (let [ring-request (:request context)
        tx (:tx ring-request)
        auth-entity (:authenticated-entity ring-request)
        req-id (-> args
                   :input_data
                   :id)
        request (get-request-by-id tx auth-entity req-id)
        field-perms (request-fields-perms/get-for-user-and-request tx
                                                                   auth-entity
                                                                   request)]
    (authorization/authorize-and-apply
      #(let [result (jdbc/execute! tx
                                   (-> (sql/delete-from :procurement_requests)
                                       (sql/where [:= :procurement_requests.id
                                                   req-id])
                                       sql-format))]
         (= result '(1)))
      :if-only
      #(:DELETE field-perms))))

(defn requested-by?
  [tx auth-entity request]
  (= (:user_id auth-entity)
     (-> requests-base-query
         (sql/where [:= :procurement_requests.id (:id request)])
         sql-format
         (->> (query-requests tx auth-entity))
         first
         :user_id)))
