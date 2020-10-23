(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.resources.inventory-pools.entitlement-groups.entitlement-group.front :as entitlement-group :refer [name-component entitlement-group-id*]]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-hierarchy]]
    [leihs.admin.shared.membership.groups.front :as groups-membership]
    [leihs.admin.utils.regex :as regex]
    [leihs.core.icons :as icons]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))



(defn member-path [group]
  (path :inventory-pool-entitlement-group-group
        {:inventory-pool-id @inventory-pool-id*
         :entitlement-group-id @entitlement-group-id*
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
   [:span "Groups of the Entitlement-Group "]
   [:a {:href (path :inventory-pool-entitlement-group
                    {:inventory-pool-id @inventory-pool-id*
                     :entitlement-group-id @entitlement-group-id*})}
    [:span [name-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div ]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount groups/escalate-query-paramas-update
     :did-update groups/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [groups/groups-table-component
    [groups-membership/member-th-component]
    [(partial groups-membership/member-td-component member-path)]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [[breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/inventory-pools-li]
      [breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*]
      [breadcrumbs/inventory-pool-entitlement-groups-li @inventory-pool-id*]
      [breadcrumbs/inventory-pool-entitlement-group-li @inventory-pool-id* @entitlement-group-id*]
      [breadcrumbs/inventory-pool-groups-li @inventory-pool/inventory-pool-id*]]
     [])
   [:div
    [header-component]
    [main-page-component]]])
