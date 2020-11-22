(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.shared :refer [default-query-params]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-component roles-hierarchy]]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


;### roles ####################################################################

(defn roles-th-component  []
  [:th {:key :roles} " Roles "])

(defn roles-td-component [group]
  (let [path (path :inventory-pool-group-roles
                   {:inventory-pool-id @inventory-pool/id*
                    :group-id (:id group)})
        has-a-role? (some->> group :roles vals (reduce #(or %1 %2)))]
    [:td {:key :roles}
     [roles-component group false nil]
     [:a.btn.btn-outline-primary.btn-sm {:href path}
      (if has-a-role?
        [:span icons/edit " Edit "]
        [:span icons/add " Add "])]]))

;### actions ##################################################################


;### filter ###################################################################


(defn form-role-filter []
  (let [role (or (-> @routing/state* :query-params :role presence)
                 (-> default-query-params :role))]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :groups-filter-role} " Role "]
     [:select#groups-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (groups/page-path-for-query-params
                                              {:page 1
                                               :role val}))))}
      (for [a  (concat ["any" "none"] roles-hierarchy)]
        [:option {:key a :value a} a])]]))

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
    [:div ]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount groups/escalate-query-paramas-update
     :did-update groups/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [groups/table-component
    [groups/name-th-component roles-th-component]
    [groups/name-td-component roles-td-component]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]
   ])

(defn index-page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch))}]
   [breadcrumbs/nav-component
    @breadcrumbs/left* [] ]
   [:div
    [:h1
     "Groups with their Roles "
     [:span " in the Inventory-Pool "]
     [inventory-pool/name-link-component]]
    [main-page-component]]])
