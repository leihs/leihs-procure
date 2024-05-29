(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom {}))
(defonce current-route* (reaction (:route @routing/state*)))

(defn fetch-entitlement-groups []
  (http-client/route-cached-fetch data*))

(defn entitlement-groups-thead []
  [:tr
   [:th.name.text-left "Name"]
   [:th.text-right "# Models "]
   [:th.text-right "# Users"]
   [:th.text-right "# Direct Users"]
   [:th.text-right "# Groups"]
   [:th.text-center]])

(defn entitlement-group-row [{id :id :as entitlement-group}]
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

(defn entitlement-groups-table []
  [:section.entitlement-entitlement-groups
   (if-not (contains? @data* @current-route*)
     [wait-component]
     (if-let [entitlement-groups (-> @data* (get  @current-route* {}) :entitlement-groups seq)]
       [table/container {:className "entitlement-groups"
                         :header (entitlement-groups-thead)
                         :body (doall (for [entitlement-group entitlement-groups]
                                        (entitlement-group-row entitlement-group)))}]
       [:div.alert.alert-info.text-center "No (more) entitlement-groups found."]))])

(defn debug-info []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn filter-section []
  [filter/container
   [:<>
    [filter/choose-user-component
     :query-params-key :including-user
     :input-options {:placeholder "email, login, or id"}]
    [filter/reset]]])

(defn page []
  [:article.inventory-pool-entitlement-groups
   [routing/hidden-state-component
    {:did-change fetch-entitlement-groups}]
   [inventory-pool/header]
   [inventory-pool/tabs]
   [filter-section]
   [table/toolbar]
   [entitlement-groups-table]
   [table/toolbar]
   [debug-info]])
