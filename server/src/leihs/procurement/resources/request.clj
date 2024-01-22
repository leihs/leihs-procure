(ns leihs.procurement.resources.request
  (:require (clojure [set :refer [map-invert]]
                     [string :refer [lower-case upper-case]])
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
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

(defn state-sql
  [advanced-user?]
  (let [s-map (states-conds-map advanced-user?)
        map (->> s-map
                 keys
                 (map name)
                 (interleave (vals s-map))
                 (cons :case))]
    [map]))

(def sql-order-by-expr
  (str "concat("
       "lower(coalesce(procurement_requests.article_name, '')), "
       "lower(coalesce(models.product, '')), "
       "lower(coalesce(models.version, ''))" ")"))

(def requests-base-query
  (let [conc sql-order-by-expr]
    (-> (sql/select [[:raw (str "DISTINCT ON (procurement_requests.id, "
                                conc
                                ") procurement_requests.*")]])
        (sql/from :procurement_requests)
        (sql/left-join :models [:= :models.id :procurement_requests.model_id])
        (sql/order-by [[:raw conc]] :procurement_requests.id))))

(defn requests-base-query-with-state
  [advanced-user?]
  (-> requests-base-query
      (sql/select [(state-sql advanced-user?) :state])
      (sql/join :procurement_budget_periods
                [:= :procurement_budget_periods.id
                 :procurement_requests.budget_period_id])))

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
    (->> (jdbc/execute! tx query)
         (map #(transform-row % advanced-user?)))))

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

(defn get-last-created-request
  [tx auth-entity]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        ; NOTE: reselect because of:
        ; ERROR: SELECT DISTINCT ON expressions must match initial ORDER BY expressions
        (sql/select :procurement_requests.* [(state-sql advanced-user?) :state])
        (sql/order-by [:created_at :desc])
        (sql/limit 1)
        sql-format
        (->> (query-requests tx auth-entity))
        first)))

(defn insert!
  [tx data]
  (jdbc/execute! tx (-> (sql/insert-into :procurement_requests)
                        (sql/values [data])
                        sql-format)))

(defn update!
  [tx req-id data]
  (jdbc/execute-one! tx (-> (sql/update :procurement_requests)
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
  [[:cast (to-name-and-lower-case a) :order_status_enum]])

(defn update-request!
  [context args _]
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
        update-data
        (as-> input-data <>
          (dissoc <> :id)
          (dissoc <> :attachments)
          (cond-> <> (:order_status <>)
                  (update :order_status
                          cast-to-order-status-enum))
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
     #(let [result (jdbc/execute-one! tx
                                      (-> (sql/delete-from :procurement_requests)
                                          (sql/where [:= :procurement_requests.id req-id])
                                          sql-format) {:builder-fn next.jdbc.result-set/as-unqualified-maps})
            result-count (:next.jdbc/update-count result)]
        (= result-count 1))
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
