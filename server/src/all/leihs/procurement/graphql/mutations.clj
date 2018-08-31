(ns leihs.procurement.graphql.mutations
  (:require
    [leihs.procurement [authorization :as authorization] [env :as env]]
    [leihs.procurement.permissions [request :as request-perms]
     [user :as user-perms]]
    [leihs.procurement.resources [admins :as admins]
     [budget-period :as budget-period] [budget-periods :as budget-periods]
     [category :as category] [categories :as categories]
     [main-categories :as main-categories] [request :as request]
     [requesters-organizations :as requesters-organizations]
     [settings :as settings] [templates :as templates]]))

(defn resolver-map-fn
  []
  {:create-request
     (fn [context args value]
       (let [rrequest (:request context)
             tx (:tx rrequest)
             auth-entity (:authenticated-entity rrequest)
             input-data (:input_data args)
             budget-period (budget-period/get-budget-period-by-id
                             tx
                             (:budget_period input-data))
             category (category/get-category-by-id tx (:category input-data))
             user-id (:user input-data)]
         (authorization/authorize-and-apply
           #(request/create-request! context args value)
           :if-only
           #(and
              (not (and category (:template input-data))) ; template belongs to
              ; category
              (not (:organization input-data)) ; implicit in user
              (not (budget-period/past? tx budget-period))
              (or (and (not user-id)
                       (user-perms/requester? tx auth-entity)
                       (budget-period/in-requesting-phase? tx budget-period))
                  (and (user-perms/requester? tx {:user_id user-id})
                       (or (user-perms/inspector? tx auth-entity (:id category))
                           (user-perms/admin? tx auth-entity)))))))),
   :change-request-budget-period
     (-> request/change-budget-period!
         (authorization/wrap-authorize-resolver
           request-perms/can-change-request-budget-period?)),
   :change-request-category (-> request/change-category!
                                (authorization/wrap-authorize-resolver
                                  request-perms/can-change-request-category?)),
   :delete-request (-> request/delete-request!
                       (authorization/wrap-authorize-resolver
                         request-perms/can-delete?)),
   :update-admins (-> admins/update-admins!
                      (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-budget-periods (-> budget-periods/update-budget-periods!
                              (authorization/wrap-ensure-one-of
                                [user-perms/admin?])),
   :update-categories-viewers (-> categories/update-categories-viewers!
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin? user-perms/inspector?])),
   :update-main-categories (-> main-categories/update-main-categories!
                               (authorization/wrap-ensure-one-of
                                 [user-perms/admin?])),
   :update-request (-> request/update-request!
                       (authorization/wrap-authorize-resolver
                         request-perms/can-edit?)),
   :update-requesters-organizations
     (-> requesters-organizations/update-requesters-organizations!
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-settings (-> settings/update-settings!
                        (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-templates (-> templates/update-templates!
                         (authorization/wrap-ensure-one-of
                           [user-perms/admin? user-perms/inspector?]))})

(def resolver-map (resolver-map-fn))

(defn get-resolver-map
  []
  (if (#{:dev :test} env/env) (resolver-map-fn) resolver-map))
