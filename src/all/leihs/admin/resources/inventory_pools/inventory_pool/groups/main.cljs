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
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.groups.shared :refer [default-query-params]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :refer [roles-component roles-hierarchy]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles-ui :as roles-ui]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


;### roles ####################################################################

(defn update-roles [group roles success-chan]
  (let  [path (path :inventory-pool-group-roles
                    {:group-id (:id group)
                     :inventory-pool-id @inventory-pool/id*})]
    (let [resp-chan (async/chan)
          id (requests/send-off {:url path
                                 :method :put
                                 :json-params {:roles roles}}
                                {:modal true
                                 :title "Update direct roles"}
                                :chan resp-chan)]
      (go (let [resp (<! resp-chan)]
            (groups/fetch-groups)
            (async/put! success-chan roles))))))

(defn roles-th-component  []
  [:th.pl-5 {:key :roles} " Roles "])

(defn roles-td-component [group]
  [:td.pl-5 {:key :roles}
   [roles-ui/inline-roles-form-component
    (get group :roles)
    (fn [roles chan] (update-roles group roles chan))]])

;### actions ##################################################################

(defn form-role-filter []
  [routing/select-component
   :label "Role"
   :query-params-key :role
   :default-option "customer"
   :options (merge {"" "(any role or none)"
                    "none" "none"}
                   (->> roles-hierarchy
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
    [:div ]))

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
    @breadcrumbs/left* [] ]
   [:div
    [:h1
     "Groups with their Roles "
     [:span " in the Inventory-Pool "]
     [inventory-pool/name-link-component]]
    [main-page-component]]])
