(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group]
    [leihs.admin.common.membership.groups.main :as groups-membership]
    [leihs.admin.utils.regex :as regex]
    [leihs.admin.common.icons :as icons]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(defn member-path [group]
  (path :inventory-pool-entitlement-group-group
        {:inventory-pool-id @inventory-pool/id*
         :entitlement-group-id @entitlement-group/id*
         :group-id (:id group)}))

;### filter ###################################################################

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [groups/form-term-filter]
    [groups/form-including-user-filter]
    [groups-membership/form-membership-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;### header ###################################################################

(defn header-component []
  [:h1
   "Groups of the Entitlement-Group "
   [entitlement-group/name-link-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div ]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [filter-component]
   [routing/pagination-component]
   [groups/table-component
    [groups/name-th-component
     groups-membership/member-th-component]
    [groups/name-td-component
     (partial groups-membership/member-td-component member-path)]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs/left*
           [breadcrumbs/groups-li])[])
   [:div
    [header-component]
    [main-page-component]]])
