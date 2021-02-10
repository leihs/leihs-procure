(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.membership.users.main :as users-membership ]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as pool-users]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [reagent.core :as reagent]))


;;; path helpers  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path [user]
  (path :inventory-pool-delegation-user
        {:inventory-pool-id @inventory-pool/id*
         :delegation-id @delegation/id* :user-id (:id user)}))

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :inventory-pool-delegation-groups
         (merge
           {:inventory-pool-id @inventory-pool/id*
            :delegation-id @delegation/id*
            :user-id (:id user)}
           more-route-params)
         (merge
           {:including-user (or (-> user :email presence) (:id user))}
           more-query-params))))

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn table-component []
  [users/table-component
   [pool-users/user-th-component
    users-membership/member-user-th-component
    users-membership/direct-member-user-th-component
    users-membership/group-member-user-th-component]
   [pool-users/user-td-component
    users-membership/member-user-td-component
    (users-membership/create-direct-member-user-td-component
      direct-member-path)
    (users-membership/create-group-member-user-td-component
      groups-path-fn)]])

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [users-membership/filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn breadcrumbs []
  [breadcrumbs/nav-component
   (conj  @breadcrumbs/left*
         [breadcrumbs/users-li])[]])

(defn index-page []
  [:div.delegation-users
   [breadcrumbs]
   [:div
    [:h1
     [:span " Users in the delegation "]
     [delegation/name-link-component]
     " in the Inventory-Pool "
     [inventory-pool/name-link-component]]
    [main-page-component]]])
