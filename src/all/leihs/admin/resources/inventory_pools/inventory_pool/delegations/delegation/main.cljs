(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.user.front]

    [leihs.admin.common.components :as components :refer [link]]
    [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation ]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.main :as delegations]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as delegation-users]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.suspension.main :as suspension]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))

;;; suspension ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-component []
  [:div#suspension
   [:h2 " Suspension " ]
   [suspension/delegation-page-suspension-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegation-component []
  [:div.delegation
   (if-let [delegation (get @delegation/data* @delegation/id*)]
     [:div
      [:table.table.table-striped.table.sm.delegation-data
       [:thead
        [:tr
         [:th "Property"]
         [:th "Value"]]]
       [:tbody
        [:tr.name [ :td "Name"] [:td.name (:name delegation)]]
        [:tr.responsible-user
         [:td "Responsible user"]
         [:td.responsible-user
          [delegation-users/user-inner-component
           (:responsible_user delegation)]]]
        [:tr.protected
         [:td "Protected"]
         [:td (if (:pool_protected delegation)
                [:span.text-success "yes"]
                [:span.text-warning "no"])]]
        [:tr.contracts-count-open-per-pool
         [:td "Number of contracts open in pool "]
         [:td.contracts-count-open-per-pool (:contracts_count_open_per_pool delegation)]]
        [:tr.contracts-count-per-pool
         [:td "Number of contracts in pool "]
         [:td.contracts-count-per-pool (:contracts_count_per_pool delegation)]]
        [:tr.contracts-count
         [:td "Number of contracts total "]
         [:td.contracts-count (:contracts_count delegation)]]
        [:tr.other-pools
         [:td "Used in the following other pools"]
         [:td [:ul
               (doall (for [pool (:other_pools delegation)]
                        (let [inner [:span "in " (:name pool)]]
                          (if-not (:is_admin  @leihs.core.user.front/state*)
                            [:span inner]
                            [:li {:key (:id pool)}
                             [:a {:href (path :inventory-pool-delegation
                                              {:inventory-pool-id (:id pool)
                                               :delegation-id (:id delegation)})}
                              inner]]))))]]]
        [:tr.users-count
         [:td "Number of users"]
         [:td.users-count (:users_count delegation)]]
        [:tr.direct-users-count
         [:td "Number of direct users"]
         [:td.direct-users-count (:direct_users_count delegation)]]
        [:tr.groups-count
         [:td "Number of groups"]
         [:td.groups-count (:groups_count delegation)]]
        [:tr.created
         [:td "Created "]
         [:td.created (-> delegation :created_at humanize-datetime-component)]]]]]
     [wait-component])])

(defn show-title-component []
  [:h1
   [:span " Delegation "]
   [delegation/name-link-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]])

(defn breadcrumbs []
  (breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/users-li]
     [breadcrumbs/groups-li]
     [breadcrumbs/edit-li]]))

(defn show-page []
  [:div.delegation
   [breadcrumbs]
   [show-title-component]
   [delegation-component]
   [:div.row
    [:div.col-md-6
     [:hr] [suspension-component]]]
   [delegation/debug-component]])
