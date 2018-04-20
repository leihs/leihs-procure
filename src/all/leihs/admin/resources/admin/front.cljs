(ns leihs.admin.resources.admin.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.front.state :as state :refer [routing-state*]]
    [leihs.admin.resources.auth.front :as auth]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths]
    [leihs.admin.utils.core :refer [keyword str presence]]
    ))

(defn page []
  [:div.admin
   (when-let [user @state/user*]
     (breadcrumbs/nav-component
       [(breadcrumbs/leihs-li)
        (breadcrumbs/admin-li)]
       [(breadcrumbs/li :admin-audits-legacy " Audits legacy ")
        (breadcrumbs/li :admin-buildings " Buildings ")
        (breadcrumbs/delegations-li)
        (breadcrumbs/li :admin-fields " Fields ")
        (breadcrumbs/li :admin-inventory-pools " Inventory Pools ")
        (breadcrumbs/li :admin-languages " Languages ")
        (breadcrumbs/li :admin-mail-templates " Mail templates ")
        (breadcrumbs/li :admin-rooms " Rooms ")
        (breadcrumbs/li :admin-settings " Settings ")
        (breadcrumbs/li :admin-statistics " Statistics ")
        (breadcrumbs/li :admin-suppliers " Suppliers ")
        (breadcrumbs/users-li)]))
   [:div
    [:h1 "Admin"]
    [:p "The application with programming interface to "
     "administrate this instance of "
     [:em " leihs"]"."]]

   (when-not @state/user*
     [auth/sign-in-form-component])

   ])
