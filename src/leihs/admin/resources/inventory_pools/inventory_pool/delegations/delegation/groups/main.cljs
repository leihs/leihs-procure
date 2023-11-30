(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]

   [leihs.admin.common.components :as components]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.common.membership.groups.main :as groups-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]

   [leihs.admin.state :as state]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defn member-path
  ([group]
   (member-path group {}))
  ([group query-params]
   (path :inventory-pool-delegation-group
         {:inventory-pool-id @inventory-pool/id*
          :delegation-id @delegation/id*
          :group-id (:id group)} query-params)))

;### header ###################################################################

(defn header-component []
  [:h1
   [:span "Groups of the Delegation  "]
   [delegation/name-link-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]])

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn table-component []
  [:div
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [groups/table-component
    [groups/name-th-component
     groups-membership/member-th-component]
    [groups/name-td-component
     (partial groups-membership/member-td-component member-path)]]])

(defn main-page-component []
  [:div
   [groups-membership/filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn breadcrumbs []
  [breadcrumbs/nav-component
   (conj  @breadcrumbs/left*
          [breadcrumbs/groups-li]) []])

(defn page []
  [:div.inventory-pool-groups
   [breadcrumbs]
   [:div
    [header-component]
    [main-page-component]]])
