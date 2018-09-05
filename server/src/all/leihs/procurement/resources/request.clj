(ns leihs.procurement.resources.request
  (:require [clojure [set :refer [map-invert]]
             [string :refer [lower-case upper-case]]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions [request-helpers :as request-perms]
             [request-fields :as request-fields-perms] [user :as user-perms]]
            [leihs.procurement.resources [attachments :as attachments]
             [budget-period :as budget-period] [category :as category]
             [requesters-organizations :as requesters] [template :as template]
             [uploads :as uploads]]
            [leihs.procurement.utils [helpers :refer [reject-keys submap?]]
             [sql :as sql]]))

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

(defn states-conds-map
  [advanced-user?]
  (let [approved-not-set [:= :procurement_requests.approved_quantity nil]
        approved-greater-equal-than-requested
          [:>= :procurement_requests.approved_quantity
           :procurement_requests.requested_quantity]
        approved-smaller-than-requested ; but greater than zero
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
  (let [s-map (states-conds-map advanced-user?)]
    (->> s-map
         keys
         (map name)
         (interleave (vals s-map))
         (cons :case)
         (apply sql/call))))

(def requests-base-query
  (-> (sql/select :procurement_requests.*)
      (sql/from :procurement_requests)
      (sql/merge-left-join :models
                           [:= :models.id :procurement_requests.model_id])
      (sql/order-by (->> [:procurement_requests.article_name :models.product
                          :models.version]
                         (map #(->> (sql/call :coalesce % "")
                                    (sql/call :lower)))
                         (sql/call :concat)))))

(defn requests-base-query-with-state
  [advanced-user?]
  (-> requests-base-query
      (sql/merge-select [(state-sql advanced-user?) :state])
      (sql/merge-join :procurement_budget_periods
                      [:= :procurement_budget_periods.id
                       :procurement_requests.budget_period_id])))

(defn to-name-and-lower-case-priorities
  [m]
  (cond-> m
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

(defn treat-priority [row] (upper-case-keyword-value row :priority))

(defn treat-inspector-priority
  [row]
  (upper-case-keyword-value row :inspector_priority))

(defn initialize-attachments-attribute
  [row]
  (assoc row :attachments :unqueried))

(defn add-total-price
  [row]
  (let [quantity (or (:order_quantity row)
                     (:approved_quantity row)
                     (:requested_quantity row))]
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

(defn transform-row
  [row]
  (-> row
      enum-state
      add-total-price
      treat-priority
      treat-inspector-priority
      initialize-attachments-attribute))

(defn query-requests [tx query] (jdbc/query tx query {:row-fn transform-row}))

(defn get-request-by-id
  [tx auth-entity id]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        (sql/where [:= :procurement_requests.id id])
        sql/format
        (->> (query-requests tx))
        first)))

(defn get-account-perms
  [context value attr]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        req (get-request-by-id tx auth-entity (:id value))
        rf-perms
          (request-fields-perms/get-for-user-and-request tx auth-entity req)
        attr-perms (attr rf-perms)]
    (->> value
         :category
         :value
         (category/get-category-by-id tx)
         attr
         (if (:read attr-perms))
         (hash-map :value)
         (merge (attr rf-perms)))))

(defn cost-center
  [context _ value]
  (get-account-perms context value :cost_center))

(defn general-ledger-account
  [context _ value]
  (get-account-perms context value :general_ledger_account))

(defn procurement-account
  [context _ value]
  (get-account-perms context value :procurement_account))

(defn get-request-by-attrs
  [tx auth-entity attrs]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        (sql/merge-where (sql/map->where-clause :procurement_requests attrs))
        sql/format
        (->> (query-requests tx))
        first)))

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
                   (not user-arg) (assoc :user (:user_id auth-entity)))]
    (as-> req-stub <>
      (request-fields-perms/get-for-user-and-request tx auth-entity <>)
      (map #(apply consider-default %) <>)
      (into {} <>)
      (assoc <> :state :NEW))))

(defn get-last-created-request
  [tx auth-entity]
  (let [advanced-user? (user-perms/advanced? tx auth-entity)]
    (-> advanced-user?
        requests-base-query-with-state
        (sql/order-by [:created_at :desc])
        (sql/limit 1)
        sql/format
        (->> (query-requests tx))
        first)))

(defn insert!
  [tx data]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_requests)
                     (sql/values [data])
                     sql/format)))

(defn update!
  [tx req-id data]
  (jdbc/execute! tx
                 (-> (sql/update :procurement_requests)
                     (sql/sset data)
                     (sql/where [:= :procurement_requests.id req-id])
                     sql/format)))

(defn- filter-attachments [m as] (filter #(submap? m %) as))

(defn deal-with-attachments!
  [tx req-id attachments]
  (let [uploads-to-delete (filter-attachments {:to_delete true,
                                               :__typename "Upload"}
                                              attachments)
        uploads-to-attachments (filter-attachments {:to_delete false,
                                                    :__typename "Upload"}
                                                   attachments)
        attachments-to-delete (filter-attachments {:to_delete true,
                                                   :__typename "Attachment"}
                                                  attachments)
        ; NOTE: just for purpose of completeness and clarity:
        ; don't do anything with existing attachments
        ; attachments-to-retain
        ; (filter-attachments {:to_delete false, :__typename "Attachment"}
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
        budget-period-id (:budget_period input-data)
        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      #(jdbc/execute! tx
                      (-> (sql/update :procurement_requests)
                          (sql/sset {:budget_period_id budget-period-id})
                          (sql/where [:= :procurement_requests.id req-id])
                          sql/format))
      :if-only
      #(request-perms/authorized-to-write-all-fields?
         tx
         auth-entity
         (reverse-exchange-attrs proc-request)
         {:budget_period budget-period-id}))
    (->> req-id
         (get-request-by-id tx auth-entity)
         reverse-exchange-attrs
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
             (sql/sset (cond-> {:category_id cat-id}
                         (not (user-perms/inspector? tx auth-entity cat-id))
                           (merge change-category-reset-attrs)))
             (sql/where [:= :procurement_requests.id req-id])
             sql/format))
      :if-only
      #(request-perms/authorized-to-write-all-fields? tx
                                                      auth-entity
                                                      (reverse-exchange-attrs
                                                        proc-request)
                                                      {:category cat-id}))
    (->> req-id
         (get-request-by-id tx auth-entity)
         reverse-exchange-attrs
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
        requester-id (or (:user input-data) (:user_id auth-entity))
        organization (requesters/get-organization-of-requester tx requester-id)
        write-data (-> input-data
                       (dissoc :attachments)
                       (assoc :user requester-id)
                       (assoc :organization (:id organization))
                       to-name-and-lower-case-priorities)]
    (with-local-vars [req-id nil]
      (authorization/authorize-and-apply
        #(do (insert! tx
                      (-> write-data
                          (cond-> template (merge data-from-template))
                          exchange-attrs))
             (var-set req-id
                      (-> (get-last-created-request tx auth-entity)
                          :id))
             (if-not (empty? attachments)
               (deal-with-attachments! tx (var-get req-id) attachments)))
        :if-only
        #(request-perms/authorized-to-write-all-fields? tx
                                                        auth-entity
                                                        input-data))
      (as-> (var-get req-id) <>
        (get-request-by-id tx auth-entity <>)
        (reverse-exchange-attrs <>)
        (request-perms/apply-permissions tx
                                         auth-entity
                                         <>
                                         #(assoc %
                                           :request-id (var-get req-id)))))))

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
         (reverse-exchange-attrs proc-request)
         (reject-keys input-data request-perms/attrs-to-exclude)))
    (as-> req-id <>
      (get-request-by-id tx auth-entity <>)
      (reverse-exchange-attrs <>)
      (request-perms/apply-permissions tx
                                       auth-entity
                                       <>
                                       #(assoc % :request-id req-id)))))

(defn delete-request!
  [context args _]
  (let [result (jdbc/execute! (-> context
                                  :request
                                  :tx)
                              (-> (sql/delete-from :procurement_requests)
                                  (sql/where [:= :procurement_requests.id
                                              (-> args
                                                  :input_data
                                                  :id)])
                                  sql/format))]
    (= result '(1))))

(defn requested-by?
  [tx auth-entity request]
  (= (:user_id auth-entity)
     (-> requests-base-query
         (sql/merge-where [:= :procurement_requests.id (:id request)])
         sql/format
         (->> (query-requests tx))
         first
         :user_id)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
