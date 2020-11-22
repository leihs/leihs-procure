(ns leihs.admin.resources.admin.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :as current-user]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.statistics.breadcrumbs :as breadcrumbs-statistics]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths]
    [leihs.admin.resources.system.breadcrumbs :as system-breadcrumbs]
    [leihs.admin.resources.inventory.breadcrumbs :as inventory-breadcrumbs]
    [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs-inventory-pools]
    ))

(defn page []
  [:div.admin
   (when-let [user @current-user/state*]
     (breadcrumbs/nav-component
       [[breadcrumbs/leihs-li]
        [breadcrumbs/admin-li]]
       [[breadcrumbs/li :admin-audits-legacy " Audits legacy " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/li :admin-buildings " Buildings " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/li :admin-fields " Fields " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/groups-li]
        [inventory-breadcrumbs/inventory-li]
        [breadcrumbs-inventory-pools/inventory-pools-li]
        [breadcrumbs/li :admin-languages " Languages " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/li :admin-mail-templates " Mail templates " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/li :admin-rooms " Rooms " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs/li :admin-settings " Settings " {} {} :authorizers [auth/admin-scopes?]]
        [breadcrumbs-statistics/statistics-li]
        [breadcrumbs/li :admin-suppliers " Suppliers " {} {} :authorizers [auth/admin-scopes?]]
        (when (:scope_system_admin_read @current-user/state*)
          [system-breadcrumbs/system-li])
        [breadcrumbs/users-li]]))
   [:div
    [:h1 "Admin"]
    [:p "The application to administrate this instance of "
     [:em " leihs"]"."]]])
