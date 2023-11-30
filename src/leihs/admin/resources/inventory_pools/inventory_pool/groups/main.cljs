(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.main
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
   [leihs.admin.common.roles.components :refer [roles-component put-roles<]]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.shared :refer [default-query-params]]

   [leihs.admin.state :as state]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

;### roles ####################################################################

(defn roles-update-handler [roles group]
  (go (swap! groups/data* assoc-in
             [(:route @routing/state*) :groups (:page-index group) :roles]
             (<! (put-roles<
                  (path :inventory-pool-group-roles
                        {:inventory-pool-id @inventory-pool/id*
                         :group-id (:id group)})
                  roles)))))

(defn roles-th-component  []
  [:th.pl-5 {:key :roles} " Roles "])

(defn roles-td-component [group]
  [:td.pl-5 {:key :roles}
   [roles-component
    (get group :roles)
    :compact true
    :update-handler #(roles-update-handler % group)]])

;### actions ##################################################################

(defn form-role-filter []
  [routing/select-component
   :label "Role"
   :query-params-key :role
   :default-option "customer"
   :options (merge {"" "(any role or none)"
                    "none" "none"}
                   (->> roles/hierarchy
                        (map (fn [%1] [%1 %1]))
                        (into {})))])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [groups/form-term-filter]
     [groups/form-including-user-filter]
     [form-role-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [filter-component]
   [routing/pagination-component]
   [groups/table-component
    [groups/name-th-component groups/users-count-th-component roles-th-component]
    [groups/name-td-component groups/users-count-td-component roles-td-component]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn index-page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   [breadcrumbs/nav-component
    @breadcrumbs/left* []]
   [:div
    [:h1
     "Groups with their Roles "
     [:span " in the Inventory-Pool "]
     [inventory-pool/name-link-component]]
    [main-page-component]]])
