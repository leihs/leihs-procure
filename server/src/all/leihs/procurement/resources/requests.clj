(ns leihs.procurement.resources.requests
  (:require [clojure.tools.logging :as log]
            [clojure set string]
            [clojure.contrib.seq :refer [find-first]]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.permissions [request :as request-perms]
             [requests :as requests-perms] [user :as user-perms]]
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
  ([context arguments value]
   (let [rrequest (:request context)
         tx (:tx rrequest)
         auth-entity (:authenticated-entity rrequest)
         advanced-user? (user-perms/advanced? tx auth-entity)]
     (requests-query-map context arguments value advanced-user?)))
  ([context arguments value advanced-user?]
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
         search-term (:search arguments)
         rrequest (:request context)
         tx (:tx rrequest)
         advanced-user? (->> rrequest
                             :authenticated-entity
                             (user-perms/advanced? tx))
         start-sqlmap (request/requests-base-query-with-state advanced-user?)]
     (cond-> start-sqlmap
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
               (request/get-where-conds-for-states state advanced-user?))
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
       search-term (search-query search-term)))))

(defn get-requests
  [context arguments value]
  (if (some #(= (% arguments) [])
            [:id :budget_period_id :category_id :inspector_priority
             :organization_id :priority :state :user_id])
    []
    (let [ring-request (:request context)
          tx (:tx ring-request)
          auth-entity (:authenticated-entity ring-request)
          advanced-user? (user-perms/advanced? tx auth-entity)
          query (as-> context <>
                  (requests-query-map <> arguments value advanced-user?)
                  (requests-perms/apply-scope tx <> auth-entity)
                  (sql/format <>))
          proc-requests (request/query-requests tx query)]
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
