(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [cljs.pprint :refer [pprint]]

   [clojure.contrib.inflect :refer [pluralize-noun]]
   [clojure.contrib.inflect :refer [pluralize-noun]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]

   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.breadcrumbs :as breadcrumbs]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom {}))
(defonce current-route* (reaction (:route @routing/state*)))

(defn fetch-entitlement-groups []
  (http-client/route-cached-fetch data*))

(defn entitlement-groups-thead-component []
  [:thead
   [:tr
    [:th.name.text-left "Name"]
    [:th.text-right "# Models "]
    [:th.text-right "# Users"]
    [:th.text-right "# Direct Users"]
    [:th.text-right "# Groups"]
    [:th.text-center]]])

(defn entitlement-group-row-component [{id :id :as entitlement-group}]
  ^{:key id}
  [:tr.entitlement-group.text-left
   [:td
    [:a {:href (path :inventory-pool-entitlement-group {:inventory-pool-id @inventory-pool/id* :entitlement-group-id id})}
     (:name entitlement-group)]]
   [:td.entitlements-count.text-right (-> entitlement-group :entitlements_count)]
   [:td.users-count.text-right
    [:a {:href (path :inventory-pool-entitlement-group-users
                     {:inventory-pool-id @inventory-pool/id*
                      :entitlement-group-id id}
                     {:membership :member})}
     (-> entitlement-group :users_count)
     " " [icons/edit] " "]]
   [:td.direct-users-count.text-right
    [:a {:href (path :inventory-pool-entitlement-group-users
                     {:inventory-pool-id @inventory-pool/id*
                      :entitlement-group-id id}
                     {:membership :direct})}
     (-> entitlement-group :direct_users_count)
     " " [icons/edit] " "]]
   [:td.groups-count.text-right
    [:a {:href (path :inventory-pool-entitlement-group-groups
                     {:inventory-pool-id @inventory-pool/id*
                      :entitlement-group-id id})}
     (-> entitlement-group :groups_count)
     " " [icons/edit] " "]]
   [:td.text-center [:a {:href  (str "/manage/" @inventory-pool/id* "/groups/" id "/edit")}
                     [icons/edit] " Edit "]]])

(defn main-page-component []
  [:div.entitlement-entitlement-groups
   (if-not (contains? @data* @current-route*)
     [wait-component]
     (if-let [entitlement-groups (-> @data* (get  @current-route* {}) :entitlement-groups seq)]
       [:table.entitlement-groups.table.table-striped.table-sm
        [entitlement-groups-thead-component]
        [:tbody
         (doall (for [entitlement-group entitlement-groups]
                  (entitlement-group-row-component entitlement-group)))]]
       [:div.alert.alert-warning.text-center "No (more) entitlement-groups found."]))])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn filter-form []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/choose-user-component
      :query-params-key :including-user
      :input-options {:placeholder "email, login, or id"}]
     [routing/form-reset-component]]]])

(defn index-page []
  [:div.inventory-pool-entitlement-groups
   [routing/hidden-state-component
    {:did-change fetch-entitlement-groups}]
   [breadcrumbs/nav-component
    @breadcrumbs/left* []]
   [:div
    [:small "See also "
     [:a
      {:href (str "/manage/" @inventory-pool/id* "/groups")}
      "Entitlement-Groups in the leihs-legacy "]
     "interface."]
    [:h1
     [:span "Entitlement-Groups of the Inventory-Pool "]
     [inventory-pool/name-link-component]]
    [filter-form]
    [main-page-component]
    [debug-component]]])
