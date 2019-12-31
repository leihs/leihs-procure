(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.front.breadcrumbs :as breadcrumbs]
     [leihs.admin.front.components :as components]
     [leihs.admin.front.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.shared :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.front :as roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.front :as suspension]
     [leihs.admin.resources.user.front.shared :as user :refer [user-id* user-data*]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))

(defonce inventory-pool-user-data* (reagent/atom nil))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@inventory-pool-user-data*"]
      [:pre (with-out-str (pprint @inventory-pool-user-data*))]]]))

(defn clean-and-fetch []
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))

(defn direct-roles-component []
  [:div.direct-roles
   [routing/hidden-state-component
    {:will-mount roles/clean-and-fetch
     :did-change roles/clean-and-fetch}]
   [:h2 [:a {:href (path :inventory-pool-user-roles
                         {:inventory-pool-id @inventory-pool-id*
                          :user-id @user-id*})}
         "Direct Roles"]]
   [roles/roles-component]])

(defn roles-via-groups-component []
  [:div.roles-via-groups
   [:h2 "Roles assigned via Groups"]
   [:p "This feature is not implemented yet."]
   ])

(defn effective-roles-component []
  [:div.effective-roles
   [:h2 "Effective Roles"]
   [:p "This section will show the effective roles derived from "
    "direct assignment and roles assigned via groups in the future. "
    "For the time being the roles given via direct assignment are excactly "
    " equal to the the effective roles."]])

(defn suspension-component []
  [:div.suspension
   [:h2
    [:a
     {:href (path :inventory-pool-user-suspension
                  {:inventory-pool-id @inventory-pool-id*
                   :user-id @user-id*})}
     "Suspension"]]
   [suspension/suspension-component]
   [suspension/remove-suspension-component]])

(defn page []
  [:div.user-roles
   [routing/hidden-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    [(breadcrumbs/leihs-li)
     (breadcrumbs/admin-li)
     (breadcrumbs/inventory-pools-li)
     (breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*)
     (breadcrumbs/inventory-pool-users-li @inventory-pool/inventory-pool-id*)
     [breadcrumbs/inventory-pool-user-li @inventory-pool-id* @user-id*]]
    [[breadcrumbs/inventory-pool-user-roles-li @inventory-pool-id* @user-id*]
     [breadcrumbs/inventory-pool-user-suspension-li @inventory-pool-id* @user-id*]
     ]]
   [:h1 "Details for "
    [user/user-name-component]
    " in "
    [inventory-pool/inventory-pool-name-component]]
   [suspension-component]
   [effective-roles-component]
   [direct-roles-component]
   [roles-via-groups-component]
   [debug-component]])
