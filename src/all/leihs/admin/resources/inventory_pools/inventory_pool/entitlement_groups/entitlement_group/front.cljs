(ns leihs.admin.resources.inventory-pools.entitlement-groups.entitlement-group.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
    [leihs.admin.resources.users.front :as users]
    [leihs.core.icons :as icons]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(defonce entitlement-group-id* (reaction (-> @routing/state* :route-params :entitlement-group-id)))
(defonce data* (reagent/atom nil))

(defonce entitlement-group-url*
  (reaction
    ; about the when: when used as here path can be called when the params
    ; are still null; this causes an exception and crashes the front-end
    (when (and @inventory-pool-id* @entitlement-group-id*)
      (path :inventory-pool-entitlement-group {:inventory-pool-id @inventory-pool-id*
                                               :entitlement-group-id @entitlement-group-id*}))))

(defn fetch-entitlement-group []
  (def fetch-entitlement-group-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        url @entitlement-group-url*
        id (requests/send-off {:url url
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Entitlement-group"
                               :handler-key :entitlement-group
                               :retry-fn #'fetch-entitlement-group}
                              :chan resp-chan)]
    (reset! fetch-entitlement-group-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-entitlement-group-id*))
            (swap! data* assoc-in [url] (:body resp)))))))

(def fetch fetch-entitlement-group)

(defn name-component []
  [:em.entitlement-group-name
   [routing/hidden-state-component
    {:did-mount fetch-entitlement-group}]
   (if-let [name (get-in @data* [@entitlement-group-url* :name])]
     [:span name]
     [:span {:style {:font-family :monospace}} (short-id @entitlement-group-id*)])])

(defn groups-count-component []
  [:span.entitlement-group-groups-count
   [routing/hidden-state-component
    {:did-mount fetch-entitlement-group}]
   (if-let [count (get-in @data* [@entitlement-group-url* :groups_count])]
     [:span count " " (pluralize-noun count "Group")]
     [:span  "?"])])

(defn users-count-component []
  [:span.entitlement-group-users-count
   [routing/hidden-state-component
    {:did-mount fetch-entitlement-group}]
   (if-let [count (get-in @data* [@entitlement-group-url* :users_count])]
     [:span count " " (pluralize-noun count "User")]
     [:span  "?"])])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn breadcrumbs-component []
  [breadcrumbs/nav-component
   [[breadcrumbs/leihs-li]
    [breadcrumbs/admin-li]
    [breadcrumbs/inventory-pools-li]
    [breadcrumbs/inventory-pool-li @inventory-pool-id*]
    [breadcrumbs/inventory-pool-entitlement-groups-li @inventory-pool-id*]
    [breadcrumbs/inventory-pool-entitlement-group-li @inventory-pool-id* @entitlement-group-id*]]
   [[:li.breadcrumb-item
     [:a.btn.btn-outline-primary.btn.btn-sm.pt-0.pb-0.pl-1.pr-1
      {:href (str "/manage/"
                  @inventory-pool-id* "/groups/"
                  @entitlement-group-id* "/edit")}
      icons/edit " Edit (in \"lending\")"]]
    [breadcrumbs/inventory-pool-entitlement-group-users-li @inventory-pool-id* @entitlement-group-id*]
    [breadcrumbs/inventory-pool-entitlement-group-groups-li @inventory-pool-id* @entitlement-group-id*]]])

(defn default-value-component [value]
  [:span (str value)])

(def table-conf
  [{:key :name
    :label "Name"}
   {:key :is_verification_required
    :label "Verification required"}
   {:key :entitlements_count
    :label "Number of models"}
   {:key :users_count
    :label "Number of users"}
   {:key :direct_users_count
    :label "Number of direct users"}
   {:key :groups_count
    :label "Number of groups"}])

(defn main-component []
  [:div
   [:h1 "Entitlement-Group" " "
    [name-component] " in the Inventory-Pool "
    [:a {:href (path :inventory-pool
                     {:inventory-pool-id @inventory-pool-id* })}
     [inventory-pool/name-component]]]
   (when-let [data (get-in @data* [@entitlement-group-url*])]
     [:div
      [:table.table.table-striped.table-sm.inventory-pool-data
       [:thead
        [:tr
         [:th "Property"]
         [:th "Value"]]]
       [:tbody
        (for [{key :key label :label value-component :value-component
               :or {value-component default-value-component}} table-conf]
          [:tr {:key key}
           [:td.label label]
           [:td.value [value-component (-> data key)]]])]]])])

(defn page []
  [:div.entitlement-group
   (when (and @inventory-pool-id* @entitlement-group-id*)
     [:div
      [breadcrumbs-component]
      [main-component]])
   [debug-component]])
