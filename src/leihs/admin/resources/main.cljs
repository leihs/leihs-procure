(ns leihs.admin.resources.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :as current-user]

    [leihs.admin.paths :as paths]
    [leihs.admin.resources.audits.breadcrumbs :as breadcrumbs-audits]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs-inventory-pools]
    [leihs.admin.resources.inventory.breadcrumbs :as breadcrumbs-inventory]
    [leihs.admin.resources.settings.breadcrumbs :as settings-breadcrumbs]
    [leihs.admin.resources.statistics.breadcrumbs :as breadcrumbs-statistics]
    [leihs.admin.resources.system.breadcrumbs :as breadcrumbs-system]
    [leihs.admin.resources.users.breadcrumbs :as breadcrumbs-users]
    [leihs.admin.state :as state]
    ))

(defn page []
  [:div.admin
   (when-let [user @current-user/state*]
     [breadcrumbs/nav-component
      @breadcrumbs/left*
      [[breadcrumbs/li :admin-audits-legacy " Audits legacy " {} {} :authorizers [auth/admin-scopes?]]
       [breadcrumbs-audits/audits-li]
       [breadcrumbs/li :admin-buildings " Buildings " {} {} :authorizers [auth/admin-scopes?]]
       [breadcrumbs/li :admin-fields " Fields " {} {} :authorizers [auth/admin-scopes?]]
       [breadcrumbs/groups-li]
       [breadcrumbs-inventory/inventory-li]
       [breadcrumbs-inventory-pools/inventory-pools-li]
       [breadcrumbs/li :admin-mail-templates " Mail templates " {} {} :authorizers [auth/admin-scopes?]]
       [breadcrumbs/li :admin-rooms " Rooms " {} {} :authorizers [auth/admin-scopes?]]
       [settings-breadcrumbs/settings-li]
       [breadcrumbs-statistics/statistics-li]
       [breadcrumbs/li :admin-suppliers " Suppliers " {} {} :authorizers [auth/admin-scopes?]]
       [breadcrumbs-system/system-li]
       [breadcrumbs-users/users-li]]])
   [:div
    [:h1 "Admin"]
    [:p "The application to administrate this instance of "
     [:em " leihs"]"."]]])
