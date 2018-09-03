(ns leihs.procurement.permissions.request
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [budget-period :as budget-period]
             [request :as request]]))

(defn can-edit?
  [context args value]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        req-id (or (-> args
                       :input_data
                       :id)
                   (:id value))
        request (request/get-request-by-id tx auth-entity req-id)
        budget-period (->> request
                           :budget_period_id
                           (budget-period/get-budget-period-by-id tx))]
    (and (not (budget-period/past? tx budget-period))
         (or (user-perms/admin? tx auth-entity)
             (user-perms/inspector? tx auth-entity (:category_id request))
             (and (user-perms/requester? tx auth-entity)
                  (request/requested-by? tx auth-entity request)
                  (budget-period/in-requesting-phase? tx budget-period))))))

; REFACTOR: reuse request field permissions
(defn can-change-request-budget-period?
  [context args value]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        req-id (or (-> args
                       :input_data
                       :id)
                   (:id value))
        request (request/get-request-by-id tx auth-entity req-id)
        budget-period-current (->> request
                                   :budget_period_id
                                   (budget-period/get-budget-period-by-id tx))
        budget-period-new (->> request
                               :budget_period_id
                               (budget-period/get-budget-period-by-id tx))]
    (and
      (not (budget-period/past? tx budget-period-current))
      (not (budget-period/past? tx budget-period-new))
      (or (user-perms/admin? tx auth-entity)
          (user-perms/inspector? tx auth-entity (:category_id request))
          (and (user-perms/requester? tx auth-entity)
               (request/requested-by? tx auth-entity request)
               (budget-period/in-requesting-phase? tx
                                                   budget-period-current))))))

; REFACTOR: reuse request field permissions
(defn can-change-request-category?
  [context args value]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        req-id (or (-> args
                       :input_data
                       :id)
                   (:id value))
        request (request/get-request-by-id tx auth-entity req-id)
        budget-period (->> request
                           :budget_period_id
                           (budget-period/get-budget-period-by-id tx))]
    (and (not (budget-period/past? tx budget-period))
         (or (user-perms/admin? tx auth-entity)
             (user-perms/inspector? tx auth-entity (:category_id request))
             (and (user-perms/requester? tx auth-entity)
                  (request/requested-by? tx auth-entity request)
                  (budget-period/in-requesting-phase? tx budget-period))))))

; REFACTOR: reuse request field permissions
(defn can-delete?
  [context args value]
  (let [rrequest (:request context)
        tx (:tx rrequest)
        auth-entity (:authenticated-entity rrequest)
        req-id (or (-> args
                       :input_data
                       :id)
                   (:id value))
        request (request/get-request-by-id tx auth-entity req-id)
        budget-period (->> request
                           :budget_period_id
                           (budget-period/get-budget-period-by-id tx))]
    (and (not (budget-period/past? tx budget-period))
         (or (user-perms/admin? tx auth-entity)
             (user-perms/inspector? tx auth-entity (:category_id request))
             (and (user-perms/requester? tx auth-entity)
                  (request/requested-by? tx auth-entity request)
                  (budget-period/in-requesting-phase? tx budget-period))))))

(defn action-permissions
  [context args value]
  {:edit (can-edit? context args value),
   :delete (can-delete? context args value),
   :moveBudgetPeriod (can-change-request-budget-period? context args value),
   :moveCategory (can-change-request-category? context args value)})
