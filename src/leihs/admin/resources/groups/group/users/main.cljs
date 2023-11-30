(ns leihs.admin.resources.groups.group.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.http-client.core :as http-client]

   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.groups.group.core :as group :refer [group-id*]]
   [leihs.admin.resources.groups.group.users.shared :refer [default-query-params]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.state :as state]
   [leihs.admin.utils.regex :as regex]
   [leihs.core.auth.core :as auth]

   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [reagent.core :as reagent]))

;### helpers ##################################################################

(def not-protected*
  (reaction
   (-> @group/data* :admin_protected not))); suffices because of hierarchy

(def is-admin-and-not-system-admin-protected*
  (reaction
   (boolean
    (and (-> @group/data* :system_admin_protected not)
         @current-user/admin?*))))

(def manage-membership-allowed?*
  (reaction
   (or @not-protected*
       @is-admin-and-not-system-admin-protected*
       @current-user/system-admin?*)))

;### actions ##################################################################

(defn add-user [{user-id :id page-index :page-index}]
  (go (when (some->
             {:chan (async/chan)
              :url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :put}
             http-client/request :chan <!
             http-client/filter-success!)
        (swap! users/data* assoc-in [(:route @routing/state*)
                                     :users page-index :group_id] @group-id*))))

(defn remove-user [{user-id :id page-index :page-index}]
  (go (when (some->
             {:chan (async/chan)
              :url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :delete}
             http-client/request :chan <!
             http-client/filter-success!)
        (swap! users/data* assoc-in [(:route @routing/state*)
                                     :users page-index :group_id] nil))))

(defn action-th-component []
  [:th.text-right "Add or remove from this group"])

(defn action-td-component [user]
  [:td.text-right
   (if (:group_id user)
     [:button.btn.btn-sm.btn-warning
      {:on-click #(remove-user user)
       :disabled (not @manage-membership-allowed?*)}
      [icons/delete] " Remove "]
     [:button.btn.btn-sm.btn-primary
      {:on-click #(add-user user)
       :disabled (not @manage-membership-allowed?*)}
      [icons/add] " Add "])])

;### filter ###################################################################

(defn form-group-users-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.mr-1 {:for :users-filter-enabled} "Membership"]
   [:select#users-filter-enabled.form-control
    {:value (or (-> @routing/state* :query-params :membership presence)
                (:membership default-query-params))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate!
                     (path :group-users {:group-id @group-id*}
                           (merge {} (:query-params @routing/state*)
                                  {:page 1
                                   :membership val})))))}
    (for [[k n] {"any" "members and non-members"
                 "yes" "members"}]
      [:option {:key k :value k} n])]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [users/form-term-filter]
     [form-group-users-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

(defn table-component []
  [users/table-component
   [users/user-th-component
    action-th-component]
   [users/user-td-component
    action-td-component]])

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div
      [:h3 "@group/data"]
      [:pre (with-out-str (pprint @group/data*))]]
     [:div
      [:h3 "@current-user/admin?*"]
      [:pre (with-out-str (pprint @current-user/admin?*))]]
     [:div
      [:h3 "(-> @group/data* :system_admin_protected not)"]
      [:pre (with-out-str (pprint (-> @group/data* :system_admin_protected not)))]]
     [:div
      [:h3 "@is-admin-and-not-system-admin-protected*"]
      [:pre (with-out-str (pprint @is-admin-and-not-system-admin-protected*))]]
     [:div
      [:h3 "@manage-membership-allowed?*"]
      [:pre (with-out-str (pprint @manage-membership-allowed?*))]]]))

(defn main-page-component []
  [:div
   (when (and @group/data*
              (not @manage-membership-allowed?*))
     [:div.alert.alert-warning
      "This group is " [:strong "protected"] ". "
      "Memebership can be inspected but not changed!"])
   [filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.group-users
   [routing/hidden-state-component
    {:did-mount (fn [_] (group/clean-and-fetch))}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/users-li])]
   [:div
    [:h1
     [:span "Users in the Group "]
     [group/group-name-component]]
    [main-page-component]]])
