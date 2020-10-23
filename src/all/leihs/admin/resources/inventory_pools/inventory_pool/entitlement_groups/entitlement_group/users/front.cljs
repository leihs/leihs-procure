(ns leihs.admin.resources.inventory-pools.entitlement-groups.entitlement-group.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.entitlement-groups.entitlement-group.front :as entitlement-group :refer [name-component entitlement-group-id*]]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.shared.membership.users.front :as membership.users :refer [form-membership-filter filter-component member-user-conf]]
    [leihs.core.icons :as icons]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path-fn [user]
  (path :inventory-pool-entitlement-group-direct-user
        {:inventory-pool-id @inventory-pool-id*
         :entitlement-group-id @entitlement-group-id*
         :user-id (:id user)}))

(def direct-member-user-conf
  {:key :direct
   :th membership.users/direct-member-user-th-component
   :td (membership.users/create-direct-member-user-td-component
         direct-member-path-fn)})


;;; group member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :inventory-pool-entitlement-group-groups
         (merge {:inventory-pool-id @inventory-pool-id*
                 :entitlement-group-id @entitlement-group-id*}
                more-route-params)
         (merge {:including-user (or (-> user :email presence) (:id user))}
                more-query-params))))

(def group-member-user-conf
  {:key :group
   :th membership.users/group-member-user-th-component
   :td (membership.users/create-group-member-user-td-component
         groups-path-fn)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def colconfig
  (merge users/default-colconfig
         {:email false
          :org_id false
          :customcols [member-user-conf
                       direct-member-user-conf
                       group-member-user-conf]}))

(defn users-component []
  [:div.entitlement-group-users
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [users/users-table-component colconfig]
   [routing/pagination-component]])

(defn header-component []
  [:h1
   [:span " Users of the Entitlement-Group "]
   [:a {:href (path :inventory-pool-entitlement-group
                    {:inventory-pool-id @inventory-pool-id*
                     :entitlement-group-id @entitlement-group-id*})}
    [:span [name-component]]]])

(defn breadcrumbs-component []
  (when (and @inventory-pool-id* @entitlement-group-id*)
    [breadcrumbs/nav-component
     [[breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/inventory-pools-li]
      [breadcrumbs/inventory-pool-li @inventory-pool-id*]
      [breadcrumbs/inventory-pool-entitlement-groups-li
       @inventory-pool-id*]
      [breadcrumbs/inventory-pool-entitlement-group-li
       @inventory-pool-id* @entitlement-group-id*]
      [breadcrumbs/inventory-pool-entitlement-group-users-li
       @inventory-pool-id* @entitlement-group-id*]][]]))

(defn page []
  [:div.inventory-pool-entitlement-group-users
   [routing/hidden-state-component
    {:did-mount entitlement-group/fetch
     :did-change entitlement-group/fetch}]
   [breadcrumbs-component]
   [header-component]
   [users-component]
   [users/debug-component]])
