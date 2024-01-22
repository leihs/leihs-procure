(ns leihs.procurement.graphql.queries
  (:require [leihs.procurement
             [authorization :as authorization]
             [dashboard :as dashboard]]
            [leihs.procurement.permissions.user :as user-perms]
            [leihs.procurement.resources
             [admins :as admins]
             [attachments :as attachments]
             [budget-limits :as budget-limits]
             [budget-period :as budget-period]
             [budget-periods :as budget-periods]
             [building :as building]
             [buildings :as buildings]
             [categories :as categories]
             [category :as category]
             [current-user :as current-user]
             [inspectors :as inspectors]
             [main-categories :as main-categories]
             [main-category :as main-category]
             [model :as model]
             [models :as models]
             [organization :as organization]
             [organizations :as organizations]
             [request :as request]
             [requesters-organizations :as requesters-organizations]
             [requests :as requests]
             [room :as room]
             [rooms :as rooms]
             [settings :as settings]
             [supplier :as supplier]
             [suppliers :as suppliers]
             [template :as template]
             [templates :as templates]
             [user :as user]
             [users :as users]
             [viewers :as viewers]]))

(def resolvers
  {:admins (-> admins/get-admins
               (authorization/wrap-ensure-one-of [user-perms/admin?])),
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
   :category category/get-category,
   :categories categories/get-categories,
   :current-user current-user/get-current-user,
   :dashboard dashboard/get-dashboard,
   :department organization/get-department-of-organization,
   :department-of-requester-organization
   organization/get-department-of-requester-organization,
   :inspectors (-> inspectors/get-inspectors
                   (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :main-category main-category/get-main-category,
   :main-categories main-categories/get-main-categories,
   :model model/get-model,
   :models models/get-models,
   :new-request request/get-new,
   :organization organization/get-organization,
   :organizations organizations/get-organizations,
   :requests requests/get-requests,
   :requesters-organizations
   (-> requesters-organizations/get-requesters-organizations
       (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :requests-count template/requests-count,
   :permissions user-perms/get-permissions,
   :room room/get-room,
   :rooms rooms/get-rooms,
   :settings settings/get-settings,
   :supplier supplier/get-supplier,
   :suppliers suppliers/get-suppliers,
   :template template/get-template,
   :templates templates/get-templates,
   :total-price-cents-new-requests (-> requests/total-price-cents-new-requests
                                       (authorization/wrap-ensure-one-of
                                        [user-perms/admin?])),
   :total-price-cents-inspected-requests
   (-> requests/total-price-cents-inspected-requests
       (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :total-price-cents-requested-quantities
   (-> requests/total-price-cents-requested-quantities
       (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :total-price-cents-approved-quantities
   (-> requests/total-price-cents-approved-quantities
       (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :total-price-cents-order-quantities
   (-> requests/total-price-cents-order-quantities
       (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :user (-> user/get-user (authorization/wrap-ensure-one-of [user-perms/admin? user-perms/inspector?])),
   :users (-> users/get-users (authorization/wrap-ensure-one-of [user-perms/admin? user-perms/inspector?])),
   :viewers (fn [context args value]
              (let [rrequest (:request context)
                    tx (:tx-next rrequest)
                    auth-entity (:authenticated-entity rrequest)]
                ((-> viewers/get-viewers
                     (authorization/wrap-ensure-one-of
                      [user-perms/admin?
                       (fn [tx auth-entity]
                         (user-perms/inspector? tx auth-entity (:id value)))]))
                 context
                 args
                 value)))})
