(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.groups.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.membership.groups.main :as groups-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]))

(defn member-path
  ([group]
   (member-path group {}))
  ([group query-params]
   (path :inventory-pool-delegation-group
         {:inventory-pool-id @inventory-pool/id*
          :delegation-id @delegation/id*
          :group-id (:id group)} query-params)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn table []
  [:<>
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [groups/table-component
    [groups/name-th-component
     groups-membership/member-th-component]
    [groups/name-td-component
     (partial groups-membership/member-td-component member-path)]]])

(defn main-section []
  [:section
   [groups-membership/filter-component]
   [table/toolbar]
   [table]
   [table/toolbar]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:article.inventory-pool-groups.my-5
   [delegation/header]
   [delegation/tabs]
   [main-section]])
