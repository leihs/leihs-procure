(ns leihs.procurement.graphql.queries
  (:require
    [leihs.procurement [authorization :as authorization] [env :as env]]
    [leihs.procurement.permissions [request :as request-perms]
     [user :as user-perms]]
    [leihs.procurement.resources [admins :as admins]
     [attachments :as attachments] [budget-limits :as budget-limits]
     [budget-period :as budget-period] [budget-periods :as budget-periods]
     [building :as building] [buildings :as buildings]
     [categories :as categories] [category :as category]
     [current-user :as current-user] [inspectors :as inspectors]
     [main-categories :as main-categories] [main-category :as main-category]
     [model :as model] [models :as models] [organization :as organization]
     [organizations :as organizations] [request :as request]
     [requesters-organizations :as requesters-organizations]
     [requests :as requests] [room :as room] [rooms :as rooms]
     [settings :as settings] [settings :as settings] [supplier :as supplier]
     [suppliers :as suppliers] [template :as template] [templates :as templates]
     [user :as user] [users :as users] [viewers :as viewers]]))

(defn resolver-map-fn
  []
  {:admins (-> admins/get-admins
               (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :request-action-permissions request-perms/action-permissions,
   :attachments attachments/get-attachments,
   :budget-limits budget-limits/get-budget-limits,
   :budget-period budget-period/get-budget-period,
   :budget-periods budget-periods/get-budget-periods,
   :buildings buildings/get-buildings,
   :building building/get-building,
   :can-delete-budget-period? (-> budget-period/can-delete?
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin?])),
   :can-delete-category? (-> category/can-delete?
                             (authorization/wrap-ensure-one-of
                               [user-perms/admin?])),
   :can-delete-main-category? (-> main-category/can-delete?
                                  (authorization/wrap-ensure-one-of
                                    [user-perms/admin?])),
   :can-delete-template? (-> template/can-delete?
                             (authorization/wrap-ensure-one-of
                               [user-perms/admin? user-perms/inspector?])),
   :category category/get-category,
   :categories categories/get-categories,
   :cost-center request/cost-center,
   :current-user current-user/get-current-user,
   :department organization/get-department,
   :general-ledger-account request/general-ledger-account,
   :inspectors (-> inspectors/get-inspectors
                   (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :main-category main-category/get-main-category,
   :main-categories main-categories/get-main-categories,
   :model model/get-model,
   :models models/get-models,
   :new-request
     (fn [context args value]
       (let [rrequest (:request context)
             tx (:tx rrequest)
             auth-entity (:authenticated-entity rrequest)
             budget-period
               (budget-period/get-budget-period-by-id tx (:budget_period args))
             category (category/get-category-by-id tx (:category args))]
         (authorization/authorize-and-apply
           #(request/get-new context args value)
           :if-only
           #(and (not (and category (:template args))) ; template belongs to
                 ; category
                 (not (budget-period/past? tx budget-period))
                 (or (user-perms/admin? tx auth-entity)
                     (and (user-perms/requester? tx auth-entity)
                          (or (and ; (:for_user args)
                                   (user-perms/inspector? tx
                                                          auth-entity
                                                          (:id category)))
                              (budget-period/in-requesting-phase?
                                tx
                                budget-period)))))))),
   :organization organization/get-organization,
   :organizations organizations/get-organizations,
   :requests requests/get-requests,
   :requesters-organizations
     (-> requesters-organizations/get-requesters-organizations
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :permissions user-perms/get-permissions,
   :procurement-account request/procurement-account,
   :room room/get-room,
   :rooms rooms/get-rooms,
   :settings settings/get-settings,
   :supplier supplier/get-supplier,
   :suppliers suppliers/get-suppliers,
   :template template/get-template,
   :templates templates/get-templates,
   :total-price-cents requests/total-price-cents,
   :total-price-cents-requested-quantities
     (-> requests/total-price-cents-requested-quantities
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :total-price-cents-approved-quantities
     (-> requests/total-price-cents-approved-quantities
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :total-price-cents-order-quantities
     (-> requests/total-price-cents-order-quantities
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :user user/get-user,
   :users users/get-users,
   :viewers (fn [context args value]
              (let [rrequest (:request context)
                    tx (:tx rrequest)
                    auth-entity (:authenticated-entity rrequest)]
                ((-> viewers/get-viewers
                     (authorization/wrap-ensure-one-of
                       [user-perms/admin?
                        (fn [tx auth-entity]
                          (user-perms/inspector? tx auth-entity (:id value)))]))
                  context
                  args
                  value)))})

(def resolver-map (resolver-map-fn))

(defn get-resolver-map
  []
  (if (#{:dev :test} env/env) (resolver-map-fn) resolver-map))
