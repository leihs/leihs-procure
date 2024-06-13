(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.main
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.roles.components :as roles-ui]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main :as direct-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main :as groups-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups.main :as groups]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main :as user-roles]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as suspension]
   [leihs.admin.resources.users.user.core :as user :refer [clean-and-fetch
                                                           user-data*]]
   [leihs.admin.resources.users.user.main :as user-main :refer [check-user-chosen]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@user-data*"]
      [:pre (with-out-str (pprint @user-data*))]]]))

;;; suspension ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn suspension-component []
  [:div#suspension
   [:h2 " Suspension "]
   [suspension/user-page-suspension-component]])

;;; roles ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn effective-roles-component []
  [:div.effective-roles
   ;; [routing/hidden-state-component
   ;;  {:did-change user-roles/clean-and-fetch}]
   [:h2 "Roles"]
   [:p "This section shows the effective roles. This is an aggregate computed from "
    "direct roles, and roles via groups. "]
   [roles-ui/roles-component @user-roles/roles-data*
    :compact false]])

(defn direct-roles-component []
  [:div.direct-roles
   [routing/hidden-state-component
    {:did-change direct-roles/fetch}]
   [:h3 [:span " Direct roles "]]
   [roles-ui/roles-component @direct-roles/roles-data*
    :compact true
    :update-handler #(go (<! (direct-roles/update-handler %))
                         (user-roles/clean-and-fetch))]])

(defn roles-via-groups-component [user]
  [:div.roles-via-groups
   [:h3 "Roles via groups "]
   [groups-roles/groups-roles-component2
    #(user-roles/clean-and-fetch)
    :user-uid (-> user :email presence)]])

(defn roles-component []
  [:div
   [effective-roles-component]
   [:div.mt-3]
   [direct-roles-component]
   [:div.mt-3]
   [roles-via-groups-component @user-data*]])

;;; overview ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups []
  [:div
   [:h3 "Groups"]
   [groups/table-component]])

(defn pool-data-li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.mb-0
    [:dt.col-sm-5 dt]
    [:dd.col-sm-7 dd]]])

(defn not-zero-or-nil [n]
  (when-not (= n 0) n))

(defn not-zero-count-pool-data-li-dl-component [user dt dd-key]
  (when-let [c (-> user
                   dd-key
                   not-zero-or-nil)]
    [pool-data-li-dl-component dt c]))

(defn overview-pool-data-component [user]
  [:div
   [:ul.list-unstyled
    [pool-data-li-dl-component "Suspended"
     (if (suspension-core/suspended?
          (-> @suspension/data* :suspended_until presence js/Date.)
          (:timestamp @state/global-state*))
       [:span.text-danger "yes"]
       [:span.text-success "no"])]
    [pool-data-li-dl-component "Roles"
     [roles-ui/roles-component @user-roles/roles-data*
      :compact true]]
    [not-zero-count-pool-data-li-dl-component user
     "Submitted reservations" :reservations_submitted_count]
    [not-zero-count-pool-data-li-dl-component user
     "Approved reservations" :reservations_approved_count]
    [not-zero-count-pool-data-li-dl-component user
     "Open contracts" :contracts_open_count]
    [not-zero-count-pool-data-li-dl-component user
     "Closed contracts" :contracts_closed_count]]])

(defn overview-component []
  (let [data @user-data*]
    (fn []
      [:div.overview
       [:div.row
        [:div.col-lg-3
         [:hr]
         [:h3 " Image / Avatar "]
         [user/img-avatar-component data]]
        [:div.col-lg-3
         [:hr]
         [:h3 "Pool Related Properties"]
         [overview-pool-data-component data]]
        [:div.col-lg-3
         [:hr]
         [:h3 "Personal Properties"]
         [user/personal-properties-component data]]
        [:div.col-lg-3
         [:hr]
         [:h3 "Account Properties"]
         [user/account-properties-component data]]
        [user-main/edit-user-button]]])))

;;; page ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header []
  (let [data @user-data*]
    (fn []
      [:header.my-5
       [breadcrumbs/main]
       [:h1.mt-3 (:firstname data) " " (:lastname data)]
       [:h6 "In Pool " [inventory-pool/name-component]]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(do
                   (clean-and-fetch)
                   (user-roles/clean-and-fetch))
     :will-unmount #(reset! user-data* nil)}]

   (if (empty? @user-data*)
     [:div.mt-5
      [wait-component]]
     [:article.user-roles
      [header]
      [overview-component]
      [:div.row
       [:div.col-md-6
        [:hr] [roles-component]]
       [:div.col-md-6
        [:hr] [suspension-component]]]
      [:div.row
       [:div.col-md-6
        [:hr] [groups]]
       [:div.col-md-6
        [:hr]
        [:h2 "Extended User Info"]
        (when-let [ext-info (-> @user-data* :extended_info presence)]
          [:pre (.stringify js/JSON (.parse js/JSON ext-info) nil 2)])]]

      [debug-component]])])
