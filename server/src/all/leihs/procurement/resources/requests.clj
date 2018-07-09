(ns leihs.procurement.resources.requests
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.contrib.seq :refer [find-first]]
            [clojure.java.jdbc :as jdbc]
            [clojure.math.numeric-tower :refer [round]]
            clojure.set
            clojure.string
            [clojure.tools.logging :as log]
            [leihs.procurement.permissions.request :as request-perms]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources.budget-period :as budget-period]
            [leihs.procurement.resources.budget-periods :as budget-periods]
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(defn search-query
  [sql-query term]
  (let [term-percent (str "%" term "%")]
    (->
      sql-query
      (sql/merge-join :rooms [:= :procurement_requests.room_id :rooms.id])
      (sql/merge-join :buildings [:= :rooms.building_id :buildings.id])
      (sql/merge-join :users [:= :procurement_requests.user_id :users.id])
      (sql/merge-where
        [:or ["~~*" :buildings.name term-percent]
         ["~~*" :procurement_requests.article_name term-percent]
         ["~~*" :procurement_requests.article_number term-percent]
         ["~~*" :procurement_requests.inspection_comment term-percent]
         ["~~*" :procurement_requests.motivation term-percent]
         ["~~*" :procurement_requests.receiver term-percent]
         ["~~*" :procurement_requests.supplier_name term-percent]
         ["~~*" :rooms.name term-percent] ["~~*" :users.firstname term-percent]
         ["~~*" :users.lastname term-percent]]))))

(defn get-id-from-parent-values
  [value resource-type]
  (some->> value
           :parent-values (find-first #(= (:resource-type %) resource-type))
           :id vector))

(defn get-id-from-current-value
  [value resource-type]
  (if (= (:resource-type value) resource-type) [(:id value)]))

(defn get-id-from-resolution-context
  [value resource-type]
  (or (get-id-from-parent-values value resource-type)
      (get-id-from-current-value value resource-type)))

(defn get-id-from-arguments
  [arguments resource-type]
  (some->> arguments
           (-> resource-type
               name
               (clojure.string/replace "-" "_")
               (str "_id")
               keyword)))

(defn get-id
  [resource-type arguments value]
  (or (get-id-from-arguments arguments resource-type)
      (get-id-from-resolution-context value resource-type)))

(defn requests-query
  [context arguments value]
  (let [id (:id arguments)
        advanced-user? (:advanced-user? context)
        category-id (get-id :category arguments value)
        budget-period-id (get-id :budget-period arguments value)
        organization-id (:organization_id arguments)
        priority (some->> arguments
                          :priority
                          (map request/to-name-and-lower-case))
        inspector-priority (some->> arguments
                                    :inspector_priority
                                    (map request/to-name-and-lower-case))
        requested-by-auth-user (:requested_by_auth_user arguments)
        from-categories-of-auth-user (:from_categories_of_auth_user arguments)
        state (:state arguments)
        state-set (:request-state-set context)
        search-term (:search arguments)]
    (sql/format
      (cond-> request/requests-base-query
        id (sql/merge-where [:in :procurement_requests.id id])
        category-id (sql/merge-where [:in :procurement_requests.category_id
                                      category-id])
        budget-period-id (sql/merge-where
                           [:in :procurement_requests.budget_period_id
                            budget-period-id])
        organization-id (sql/merge-where [:in
                                          :procurement_requests.organization_id
                                          organization-id])
        priority (sql/merge-where [:in :procurement_requests.priority priority])
        inspector-priority (sql/merge-where
                             [:in :procurement_requests.inspector_priority
                              inspector-priority])
        state (sql/merge-where [:in
                                (request/state-sql (:state-value-range-type
                                                     context)) state])
        requested-by-auth-user
          (sql/merge-where [:= :procurement_requests.user_id
                            (-> context
                                :request
                                :authenticated-entity
                                :id)])
        from-categories-of-auth-user
          (sql/merge-where
            [:in :procurement_requests.category_id
             (-> (sql/select :category_id)
                 (sql/from :procurement_category_inspectors)
                 (sql/merge-where [:= :procurement_category_inspectors.user_id
                                   (-> context
                                       :request
                                       :authenticated-entity
                                       :id)]))])
        search-term (search-query search-term)))))

(defn valid-state-values-combination?
  [state-value-range-type state-arg]
  (let [valid-states (state-value-range-type request/valid-state-ranges)]
    (clojure.set/subset? (->> state-arg
                              (map keyword)
                              set)
                         valid-states)))

(defn sanitize-and-enhance-context
  [context arguments value]
  "It returns context unchanged if `state` arg not provided.
  It throws if `state` argument is provided but `budget_period_id` is missing
  or `state` arg has not an allowed value in combination with `budget_period_id`.
  Otherwise it enhances the context with additional key/val pair."
  (if-let [state-arg (:state arguments)]
    (let [rrequest (:request context)
          tx (:tx rrequest)
          auth-user (:authenticated-entity rrequest)
          budget-period-arg (:budget_period_id arguments)
          budget-periods (map #(budget-period/get-budget-period-by-id tx %)
                           budget-period-arg)
          phase-of-budget-periods
            (budget-periods/get-phase-of-budget-periods tx budget-periods)
          state-value-range-type (request/state-value-range-type
                                   tx
                                   auth-user
                                   phase-of-budget-periods)]
      (cond
        (nil? budget-period-arg)
          (throw
            (Exception.
              "One must provide budget_period_id in combination with state."))
        (= phase-of-budget-periods :mixed)
          (throw
            (Exception.
              "One cannot mix past budget periods with the current or future ones in combination with state."))
        (not (valid-state-values-combination? state-value-range-type state-arg))
          (throw
            (Exception.
              "Invalid state combinations for budget_period_ids and user permissions."))
        true (assoc context :state-value-range-type state-value-range-type)))
    context))

(defn get-requests
  [context arguments value]
  (if (some #(= (% arguments) [])
            [:id :budget_period_id :category_id :inspector_priority
             :organization_id :priority :state :user_id])
    []
    (let [sanitized-and-enhanced-context
            (sanitize-and-enhance-context context arguments value)
          ring-request (:request sanitized-and-enhanced-context)
          tx (:tx ring-request)
          proc-requests
            (jdbc/query
              tx
              (requests-query sanitized-and-enhanced-context arguments value)
              {:row-fn #(request/transform-row tx
                                               (:authenticated-entity
                                                 ring-request)
                                               %)})]
      (->> proc-requests
           (map request/reverse-exchange-attrs)
           (map #(request-perms/apply-permissions tx
                                                  (:authenticated-entity
                                                    ring-request)
                                                  %))))))

(defn total-price-query
  [qty-type bp-id]
  (-> (sql/select :pr.budget_period_id
                  [(sql/call :sum
                             (sql/call :*
                                       :pr.price_cents
                                       (->> qty-type
                                            name
                                            (str "pr.")
                                            keyword))) :result])
      (sql/from [:procurement_requests :pr])
      (sql/merge-where [:= :pr.budget_period_id bp-id])
      (sql/group :pr.budget_period_id)
      sql/format))

(defn get-total-price-cents
  [tx qty-type bp-id]
  (or (some-> (total-price-query qty-type bp-id)
              (->> (jdbc/query tx))
              first
              :result
              (/ 100)
              round)
      0))

(defn total-price-cents-requested-quantities
  [context _ value]
  (get-total-price-cents (-> context
                             :request
                             :tx)
                         :requested_quantity
                         (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (get-total-price-cents (-> context
                             :request
                             :tx)
                         :approved_quantity
                         (:id value)))

(defn total-price-cents-order-quantities
  [context _ value]
  (get-total-price-cents (-> context
                             :request
                             :tx)
                         :order_quantity
                         (:id value)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
