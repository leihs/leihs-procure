(ns leihs.admin.resources.main
  (:refer-clojure :exclude [str keyword]))

(defn page []
  [:article.admin.my-5
   #_(when-let [user @current-user/state*]
       [breadcrumbs/nav-component
        @breadcrumbs/left*
        [[breadcrumbs-audits/audits-li]
         [breadcrumbs/buildings-li]
         [breadcrumbs/groups-li]
         [breadcrumbs-inventory/inventory-li]
         [breadcrumbs/inventory-fields-li]
         [breadcrumbs-inventory-pools/inventory-pools-li]
         [breadcrumbs/mail-templates-li]
         [breadcrumbs/rooms-li]
         [settings-breadcrumbs/settings-li]
         [breadcrumbs-statistics/statistics-li]
         [breadcrumbs/suppliers-li]
         [breadcrumbs-system/system-li]
         [breadcrumbs-users/users-li]]])

   [:div
    [:h1 "Admin"]
    [:p "The application to administrate this instance of "
     [:em " leihs"] "."]]])
