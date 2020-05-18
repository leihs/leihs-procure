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
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.front :as roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.front :as direct-roles]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.front :as groups-roles]
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


(defn effective-roles-component []
  [:div.effective-roles
   [routing/hidden-state-component
    {:did-mount roles/clean-and-fetch
     :did-change roles/clean-and-fetch}]
   [:h2 "Roles"]
   [:p "This section shows the roles aggregated from "
    "direct roles, and those via groups. "]
   [roles/roles-component]])


(defn direct-roles-component []
  [:div.direct-roles
   [routing/hidden-state-component
    {:did-mount direct-roles/clean-and-fetch
     :did-change direct-roles/clean-and-fetch}]
   [:h2 [:a {:href (path :inventory-pool-user-direct-roles
                         {:inventory-pool-id @inventory-pool-id*
                          :user-id @user-id*})}
         "Direct Roles"]]
   [direct-roles/roles-component]])


(defn roles-via-groups-component []
  [:div.roles-via-groups
   [:h2 "Roles assigned via Groups"]
   [groups-roles/groups-roles-component]])

(defn page []
  [:div.user-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [breadcrumbs/inventory-pools-li]
     [breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*]
     [breadcrumbs/inventory-pool-users-li @inventory-pool/inventory-pool-id*]
     [breadcrumbs/inventory-pool-user-li @inventory-pool-id* @user-id*]]
    [[breadcrumbs/inventory-pool-user-direct-roles-li @inventory-pool-id* @user-id*]
     [breadcrumbs/inventory-pool-user-suspension-li @inventory-pool-id* @user-id*]]]
   [:h1 "Details for "
    [:a {:href (path :user {:user-id @user-id*})}
     [user/user-name-component]]
    " in "
    [:a {:href (path :inventory-pool
                     {:inventory-pool-id @inventory-pool-id*})}
     [inventory-pool/inventory-pool-name-component]]]
   [:hr]
   [suspension-component]
   [:hr]
   [effective-roles-component]
   [:hr]
   [direct-roles-component]
   [roles-via-groups-component]
   [debug-component]])
