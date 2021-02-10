(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.icons :as icons]

    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group]
    [leihs.admin.resources.users.main :as users]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))

(defn groups-count-component []
  [:span.entitlement-group-groups-count
   [routing/hidden-state-component
    {:did-mount entitlement-group/fetch}]
   (if-let [count (:groups_count @entitlement-group/data*)]
     [:span count " " (pluralize-noun count "Group")]
     [:span  "?"])])

(defn users-count-component []
  [:span.entitlement-group-users-count
   [routing/hidden-state-component
    {:did-mount entitlement-group/fetch}]
   (if-let [count (:users_count @entitlement-group/data*)]
     [:span count " " (pluralize-noun count "User")]
     [:span  "?"])])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @entitlement-group/data*))]]]))

(defn breadcrumbs-component []
  [breadcrumbs/nav-component
   @breadcrumbs/left*
   [[:li.breadcrumb-item
     [:a.btn.btn-outline-primary.btn.btn-sm.pt-0.pb-0.pl-1.pr-1
      {:href (str "/manage/"
                  @inventory-pool/id* "/groups/"
                  @entitlement-group/id* "/edit")}
      icons/edit " Edit (in \"lending\")"]]
    [breadcrumbs/users-li]
    [breadcrumbs/groups-li]]])

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

(defn title-component []
  [:h1 "Entitlement-Group "
   [entitlement-group/name-link-component]
   " in the Inventory-Pool "
   [inventory-pool/name-link-component]

   ])

(defn main-component []
  [:div
   [title-component]
   (when-let [data @entitlement-group/data*]
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
   (when (and @inventory-pool/id* @entitlement-group/id*)
     [:div
      [breadcrumbs-component]
      [main-component]
      ])
   [debug-component]])
