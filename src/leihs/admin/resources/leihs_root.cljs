(ns leihs.admin.resources.leihs-root
  (:require [leihs.admin.resources.breadcrumbs :as breadcrumbs]
            [leihs.core.user.front :as core-user]))

(defn page []
  [:div.home
   (when @core-user/state*
     (breadcrumbs/nav-component
      [[breadcrumbs/leihs-li]]
      [[breadcrumbs/admin-li]
       [breadcrumbs/borrow-li]
       [breadcrumbs/lending-li]
       [breadcrumbs/procurement-li]]))
   [:h1 "leihs-admin Home"]
   [:p.text-danger "This page is only accessible for development and testing."]])
