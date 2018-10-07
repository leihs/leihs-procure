(ns leihs.admin.resources.admin.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :as core-user]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths]
    [leihs.admin.resources.authentication-systems.breadcrumbs :as authentication-systems.breadcrumbs]
    [leihs.admin.resources.system-admins.breadcrumbs :as system-admins]
    ))

(defn page []
  [:div.admin
   (when-let [user @core-user/state*]
     (breadcrumbs/nav-component
       [(breadcrumbs/leihs-li)
        (breadcrumbs/admin-li)]
       [(breadcrumbs/li :admin-audits-legacy " Audits legacy ")
        (authentication-systems.breadcrumbs/authentication-systems-li)
        (breadcrumbs/li :admin-buildings " Buildings ")
        (breadcrumbs/delegations-li)
        (breadcrumbs/li :admin-fields " Fields ")
        (breadcrumbs/groups-li)
        (breadcrumbs/li :admin-inventory-pools " Inventory Pools ")
        (breadcrumbs/li :admin-languages " Languages ")
        (breadcrumbs/li :admin-mail-templates " Mail templates ")
        (breadcrumbs/li :admin-rooms " Rooms ")
        (breadcrumbs/li :admin-settings " Settings ")
        (breadcrumbs/li :admin-statistics " Statistics ")
        (breadcrumbs/li :admin-suppliers " Suppliers ")
        (when (:scope_system_admin_read @core-user/state*)
          (system-admins/system-admins-li))
        (breadcrumbs/users-li)]))
   [:div
    [:h1 "Admin"]
    [:p "The application to administrate this instance of "
     [:em " leihs"]"."]]])
