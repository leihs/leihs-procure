(ns leihs.procurement.graphql.queries
  (:require [leihs.procurement.authorization :as authorization]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources [admins :as admins]
             [budget-limits :as budget-limits] [budget-period :as budget-period]
             [budget-periods :as budget-periods] [building :as building]
             [buildings :as buildings] [categories :as categories]
             [category :as category] [current-user :as current-user]
             [inspectors :as inspectors] [main-categories :as main-categories]
             [main-category :as main-category] [model :as model]
             [organization :as organization] [organizations :as organizations]
             [request :as request]
             [requesters-organizations :as requesters-organizations]
             [requests :as requests] [room :as room] [rooms :as rooms]
             [settings :as settings] [supplier :as supplier] [user :as user]
             [template :as template] [templates :as templates] [users :as users]
             [viewers :as viewers]]))

; FIXME: a function for debugging convenience. will be a var later.
(defn query-resolver-map
  []
  {:admins (-> admins/get-admins
               (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :budget-limits budget-limits/get-budget-limits,
   :budget-period budget-period/get-budget-period,
   :budget-periods budget-periods/get-budget-periods,
   :buildings buildings/get-buildings,
   :building building/get-building,
   :building-rooms rooms/get-building-rooms,
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
   :inspectors inspectors/get-inspectors,
   :main-category main-category/get-main-category,
   :main-categories main-categories/get-main-categories,
   :model model/get-model,
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
   :settings (-> settings/get-settings
                 (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :supplier supplier/get-supplier,
   :templates templates/get-templates,
   :total-price-cents-requested-quantities
     requests/total-price-cents-requested-quantities,
   :total-price-cents-approved-quantities
     requests/total-price-cents-approved-quantities,
   :total-price-cents-order-quantities
     requests/total-price-cents-order-quantities,
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
                          (user-perms/inspector? tx auth-entity (:id value)))])
                     context
                     args
                     value))))})
