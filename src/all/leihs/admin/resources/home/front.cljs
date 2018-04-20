(ns leihs.admin.resources.home.front
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.admin.front.state :as state :refer [routing-state*]]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.utils.core :refer [keyword str presence]]))


(defn page []
  [:div.home

   (when-let [user @state/user*]
     (breadcrumbs/nav-component
       [(breadcrumbs/leihs-li)]
       [(breadcrumbs/admin-li)
        (breadcrumbs/borrow-li)
        (breadcrumbs/lending-li)
        (breadcrumbs/procurement-li)]))


   [:h1 "leihs - Equipment Booking and Inventory Management System"]
   [:p  "Manage inventory, place reservations on items and pick them up."]])
