(ns leihs.procurement.resources.request
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.tools.logging :as log]
            [clojure.set :refer [map-invert]]
            [clojure.string :refer [lower-case upper-case]]
            [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.request :as request-perms]
            [leihs.procurement.permissions.request-fields :as
             request-fields-perms]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources.attachments :as attachments]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.resources.category :as category]
            [leihs.procurement.resources.model :as model]
            [leihs.procurement.resources.room :as room]
            [leihs.procurement.resources.supplier :as supplier]
            [leihs.procurement.resources.uploads :as uploads]
            [leihs.procurement.utils.helpers :refer [submap?]]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [leihs.procurement.utils.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [logbug.debug :as debug]))

(def attrs-mapping
  {:budget_period :budget_period_id,
   :category :category_id,
   :model :model_id,
   :organization :organization_id,
   :room :room_id,
   :supplier :supplier_id,
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

(def valid-state-ranges
  {:normal #{:new :approved :partially_approved :denied},
   :restricted #{:new :in_approval}})

(defn state-value-range-type
  [tx user phase-of-budget-periods]
  (if (or (user-perms/advanced? tx user) (= phase-of-budget-periods :past))
    :normal
    :restricted))

(defn state-sql
  [state-set-type]
  (case state-set-type
    :normal (sql/call :case
                      [:= :procurement_requests.approved_quantity nil]
                      "new"
                      [:= :procurement_requests.approved_quantity 0]
                      "denied"
                      [:< :procurement_requests.approved_quantity
                       :procurement_requests.requested_quantity]
                      "partially_approved"
                      [:>= :procurement_requests.approved_quantity
                       :procurement_requests.requested_quantity]
                      "approved")
    :restricted (sql/call :case
                          [:= :procurement_requests.approved_quantity nil]
                          "new"
                          :else
                          "in_approval")
    (throw (Exception. "Unknown request state set."))))

(def requests-base-query
  (-> (sql/select :procurement_requests.*)
      (sql/from :procurement_requests)))

(defn get-state
  [tx auth-entity row]
  (if-let [approved-quantity (:approved_quantity row)]
    (let [budget-period
            (budget-period/get-budget-period-by-id tx (:budget_period_id row))
          range-type (state-value-range-type tx auth-entity [budget-period])
          requested-quantity (:requested_quantity row)]
      (case range-type
        :restricted "in_approval"
        :normal (cond (= 0 approved-quantity) "denied"
                      (< approved-quantity requested-quantity)
                        "partially_approved"
                      (<= requested-quantity approved-quantity) "approved")))
    "new"))

(defn add-state
  [tx auth-entity row]
  (assoc row :state (get-state tx auth-entity row)))

(defn to-name-and-lower-case
  [x]
  (-> x
      name
      lower-case))

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

(defn transform-row
  [tx auth-entity row]
  (->> row
       (add-state tx auth-entity)
       treat-priority
       treat-inspector-priority
       initialize-attachments-attribute))

(defn query-requests
  [tx auth-entity query]
  (jdbc/query tx query {:row-fn #(transform-row tx auth-entity %)}))

(defn get-request-by-id
  [tx auth-entity id]
  (-> requests-base-query
      (sql/where [:= :procurement_requests.id id])
      sql/format
      (->> (query-requests tx auth-entity))
      first))

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
  (-> requests-base-query
      (sql/merge-where (sql/map->where-clause :procurement_requests attrs))
      sql/format
      (->> (query-requests tx auth-entity))
      first))

(defn- consider-default
  [attr p-spec]
  (if (:value p-spec)
    {attr p-spec}
    {attr (assoc p-spec :value (:default p-spec))}))

(defn get-new
  [context args value]
  (let [ring-req (:request context)
        tx (:tx ring-req)
        auth-entity (:authenticated-entity ring-req)
        user-arg (:user args)
        template-arg (:template args)
        req-stub (cond-> args
                   template-arg (assoc :category template-arg)
                   (not user-arg) (assoc :user auth-entity))]
    (->> req-stub
         (request-fields-perms/get-for-user-and-request tx auth-entity)
         (map #(apply consider-default %))
         (into {}))))

(defn get-last-created-request
  [tx auth-entity]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/order-by [:created_at :desc])
      (sql/limit 1)
      sql/format
      (->> (query-requests tx auth-entity))
      first))

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
      #(jdbc/execute! tx
                      (-> (sql/update :procurement_requests)
                          (sql/sset {:category_id cat-id})
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
  (let [input-data (:input_data args)
        write-data (to-name-and-lower-case-priorities input-data)
        attachments (:attachments write-data)
        write-data-with-exchanged-attrs (-> write-data
                                            (dissoc :attachments)
                                            exchange-attrs)
        ring-req (:request context)
        tx (:tx ring-req)
        auth-entity (:authenticated-entity ring-req)]
    (with-local-vars [req-id nil]
      (authorization/authorize-and-apply
        #(do (insert! tx write-data-with-exchanged-attrs)
             (var-set req-id
                      (-> (get-last-created-request tx auth-entity)
                          :id))
             (if-not (empty? attachments)
               (deal-with-attachments! tx (var-get req-id) attachments)))
        :if-only
        #(request-perms/authorized-to-write-all-fields? tx
                                                        auth-entity
                                                        write-data))
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
        update-data
          (as-> input-data <>
            (dissoc <> :id)
            (dissoc <> :attachments)
            (cond-> <> (:priority <>) (update :priority to-name-and-lower-case))
            (cond-> <>
              (:inspector_priority <>) (update :inspector_priority
                                               to-name-and-lower-case)))
        proc-request (get-request-by-id tx auth-entity req-id)]
    (authorization/authorize-and-apply
      #(do (update! tx req-id update-data)
           (if-not (empty? attachments)
             (deal-with-attachments! tx req-id attachments)))
      :if-only
      #(request-perms/authorized-to-write-all-fields? tx
                                                      auth-entity
                                                      (reverse-exchange-attrs
                                                        proc-request)
                                                      update-data))
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
         (->> (query-requests tx auth-entity))
         first
         :user_id)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
