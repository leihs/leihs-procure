(ns leihs.procurement.resources.requests
  (:require [clojure.tools.logging :as log]
            [clojure set string]
            [clojure.contrib.seq :refer [find-first]]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.permissions [request :as request-perms]
             [requests :as requests-perms]]
            [leihs.procurement.resources [budget-period :as budget-period]
             [budget-periods :as budget-periods] [request :as request]]
            [leihs.procurement.utils.sql :as sql]))

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
  (let [id-from-args (get-id-from-arguments arguments resource-type)
        id-from-context (get-id-from-resolution-context value resource-type)]
    (if (and id-from-args id-from-context)
      (throw
        (Exception.
          "Value can not be derived from both, resolution context and arguments.")))
    (or id-from-args id-from-context)))

(defn requests-query-map
  [context arguments value]
  (let [id (:id arguments)
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
    (cond-> request/requests-base-query
      id (sql/merge-where [:in :procurement_requests.id id])
      category-id (sql/merge-where [:in :procurement_requests.category_id
                                    category-id])
      budget-period-id (sql/merge-where [:in
                                         :procurement_requests.budget_period_id
                                         budget-period-id])
      organization-id (sql/merge-where [:in
                                        :procurement_requests.organization_id
                                        organization-id])
      priority (sql/merge-where [:in :procurement_requests.priority priority])
      inspector-priority (sql/merge-where
                           [:in :procurement_requests.inspector_priority
                            inspector-priority])
      state (sql/merge-where
              [:in (request/state-sql (:state-value-range-type context)) state])
      requested-by-auth-user (sql/merge-where [:= :procurement_requests.user_id
                                               (-> context
                                                   :request
                                                   :authenticated-entity
                                                   :user_id)])
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
      search-term (search-query search-term))))

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
          auth-entity (:authenticated-entity rrequest)
          budget-period-arg (get-id :budget-period arguments value)
          budget-periods (map #(budget-period/get-budget-period-by-id tx %)
                           budget-period-arg)
          phase-of-budget-periods
            (budget-periods/get-phase-of-budget-periods tx budget-periods)
          state-value-range-type (request/state-value-range-type
                                   tx
                                   auth-entity
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
          auth-entity (:authenticated-entity ring-request)
          query (as-> sanitized-and-enhanced-context <>
                  (requests-query-map <> arguments value)
                  (requests-perms/apply-scope tx <> auth-entity)
                  (sql/format <>))
          proc-requests (jdbc/query tx
                                    query
                                    {:row-fn #(request/transform-row
                                                tx
                                                (:authenticated-entity
                                                  ring-request)
                                                %)})]
      (->> proc-requests
           (map request/reverse-exchange-attrs)
           (map (fn [proc-req]
                  (request-perms/apply-permissions
                    tx
                    (:authenticated-entity ring-request)
                    proc-req
                    #(assoc % :request-id (:id proc-req)))))))))

(defn get-total-price-cents
  [tx sqlmap]
  (or (some->> sqlmap
               sql/format
               (jdbc/query tx)
               first
               :result)
      0))

(defn get-category-id
  [context value]
  (let [tx (-> context
               :request
               :tx)
        main-category-id (get-id-from-current-value value :main-category)
        arg-ids-from-query (some-> context
                                   :categories-args
                                   :id)]
    (case (:resource-type value)
      :budget-period arg-ids-from-query
      :main-category
        (let [ids-from-all-subcategories (-> (sql/select :id)
                                             (sql/from :procurement_categories)
                                             (sql/where [:= :main_category_id
                                                         (:id value)])
                                             sql/format
                                             (->> (jdbc/query tx))
                                             (->> (map #(-> %
                                                            :id
                                                            .toString))))]
          (into []
                (clojure.set/intersection (set arg-ids-from-query)
                                          (set ids-from-all-subcategories))))
      :category [(:id value)])))

(defn total-price-cents
  [context _ value]
  (let [ring-request (:request context)
        tx (:tx ring-request)
        auth-entity (:authenticated-entity ring-request)
        budget-period-id (get-id-from-resolution-context value :budget-period)
        category-id (get-category-id context value)
        requests-args (:requests-args context)]
    (as-> {} <>
      (cond-> <>
        (not-empty budget-period-id) (assoc :budget_period_id budget-period-id))
      (cond-> <> (not-empty category-id) (assoc :category_id category-id))
      (merge <> requests-args)
      (requests-query-map context <> nil)
      (requests-perms/apply-scope tx <> auth-entity)
      (sql/select <>
                  [(->> [:order_quantity :approved_quantity :requested_quantity]
                        (apply sql/call :coalesce)
                        (sql/call :* :price_cents)
                        (sql/call :sum)) :result])
      (get-total-price-cents tx <>))))

(defn total-price-sqlmap
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
      (sql/group :pr.budget_period_id)))

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
                              :requested_quantity
                              (:id value)))

(defn total-price-cents-approved-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :approved_quantity
                              (:id value)))

(defn total-price-cents-order-quantities
  [context _ value]
  (specific-total-price-cents (-> context
                                  :request
                                  :tx)
                              :order_quantity
                              (:id value)))
