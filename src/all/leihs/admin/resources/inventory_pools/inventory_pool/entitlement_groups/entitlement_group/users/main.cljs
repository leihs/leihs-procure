(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.membership.users.main :as membership-users :refer []]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as pool-users]
    [leihs.admin.resources.users.main :as users]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path-fn [user]
  (path :inventory-pool-entitlement-group-direct-user
        {:inventory-pool-id @inventory-pool/id*
         :entitlement-group-id @entitlement-group/id*
         :user-id (:id user)}))


;;; group member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :inventory-pool-entitlement-group-groups
         (merge {:inventory-pool-id @inventory-pool/id*
                 :entitlement-group-id @entitlement-group/id*}
                more-route-params)
         (merge {:including-user (or (-> user :email presence) (:id user))}
                more-query-params))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-component []
  [:div.entitlement-group-users
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [membership-users/filter-component]
   [routing/pagination-component]
   [users/table-component
    [pool-users/user-th-component
     membership-users/member-user-th-component
     membership-users/direct-member-user-th-component
     membership-users/group-member-user-th-component]
    [pool-users/user-td-component
     membership-users/member-user-td-component
     (membership-users/create-direct-member-user-td-component
       direct-member-path-fn)
     (membership-users/create-group-member-user-td-component
       groups-path-fn)]]
   [routing/pagination-component]])

(defn header-component []
  [:h1
   [:span " Users of the Entitlement-Group "]
   [entitlement-group/name-link-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]])

(defn breadcrumbs-component []
  (when (and @inventory-pool/id* @entitlement-group/id*)
    [breadcrumbs/nav-component
     (conj @breadcrumbs/left* [breadcrumbs/users-li])
     []]))

(defn page []
  [:div.inventory-pool-entitlement-group-users
   [breadcrumbs-component]
   [header-component]
   [users-component]
   [users/debug-component]])
