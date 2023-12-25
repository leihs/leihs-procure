(ns leihs.procurement.graphql.mutations
  (:require
    [leihs.procurement [authorization :as authorization]]
    [leihs.procurement.permissions [user :as user-perms]]

        [taoensso.timbre :refer [debug info warn error spy]]


    [leihs.procurement.resources [admins :as admins]
     [budget-period :as budget-period] [budget-periods :as budget-periods]
     [category :as category] [categories :as categories]
     [main-categories :as main-categories] [request :as request]
     [requesters-organizations :as requesters-organizations]
     [settings :as settings] [templates :as templates]]))

; NOTE: some resolvers perform authorization by themselves
(def resolvers
  {:create-request request/create-request!,
   :change-request-budget-period request/change-budget-period!,
   :change-request-category request/change-category!,
   :delete-request request/delete-request!,
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
   :update-request (spy request/update-request!),
   :update-requesters-organizations
     (-> requesters-organizations/update-requesters-organizations!
         (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-settings (-> settings/update-settings!
                        (authorization/wrap-ensure-one-of [user-perms/admin?])),
   :update-templates (-> templates/update-templates!
                         (authorization/wrap-ensure-one-of
                           [user-perms/admin? user-perms/inspector?]))})
