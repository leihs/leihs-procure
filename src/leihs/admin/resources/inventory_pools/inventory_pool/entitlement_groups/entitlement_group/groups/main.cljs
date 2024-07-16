(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.groups.main
  (:require [leihs.admin.common.components.filter :as filter]
            [leihs.admin.common.components.table :as table]
            [leihs.admin.common.membership.groups.main :as groups-membership]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.groups.main :as groups]
            [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
            [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group]
            [leihs.admin.state :as state]
            [leihs.core.routing.front :as routing]))

(defn member-path [group]
  (path :inventory-pool-entitlement-group-group
        {:inventory-pool-id @pool-core/id*
         :entitlement-group-id @entitlement-group/id*
         :group-id (:id group)}))

;### filter ###################################################################

(defn filter-section []
  [filter/container
   [:<>
    [groups/form-term-filter]
    [groups/form-including-user-filter]
    [groups-membership/form-membership-filter]
    [filter/form-per-page]
    [filter/reset]]])

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn groups-table-section []
  [:section
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [filter-section]
   [table/toolbar]
   [groups/table-component
    [groups/name-th-component
     groups-membership/member-th-component]
    [groups/name-td-component
     (partial groups-membership/member-td-component member-path)]]
   [table/toolbar]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:article.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn []
                  (pool-core/clean-and-fetch))}]

   [entitlement-group/header]
   [entitlement-group/tabs]
   [groups-table-section]])
