(ns leihs.admin.resources.groups.group.users.main
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :as group-core :refer [group-id*]]
   [leihs.admin.resources.groups.group.main :as group]
   [leihs.admin.resources.groups.group.users.shared :refer [default-query-params]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [react-bootstrap :as react-bootstrap :refer [Nav]]
   [reagent.core :refer [reaction]]))

;### helpers ##################################################################

(def not-protected*
  (reaction
   (-> @group-core/data* :admin_protected not))); suffices because of hierarchy

(def is-admin-and-not-system-admin-protected*
  (reaction
   (boolean
    (and (-> @group-core/data* :system_admin_protected not)
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
   [:select#users-filter-enabled.custom-select
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
  [filter/container
   [:<>
    [users/form-term-filter]
    [form-group-users-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]])

(defn table-component []
  [users/users-table
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
      [:pre (with-out-str (pprint @group-core/data*))]]
     [:div
      [:h3 "@current-user/admin?*"]
      [:pre (with-out-str (pprint @current-user/admin?*))]]
     [:div
      [:h3 "(-> @group/data* :system_admin_protected not)"]
      [:pre (with-out-str (pprint (-> @group-core/data* :system_admin_protected not)))]]
     [:div
      [:h3 "@is-admin-and-not-system-admin-protected*"]
      [:pre (with-out-str (pprint @is-admin-and-not-system-admin-protected*))]]
     [:div
      [:h3 "@manage-membership-allowed?*"]
      [:pre (with-out-str (pprint @manage-membership-allowed?*))]]]))

(defn main-page-component []
  [:div
   (when (and @group-core/data*
              (not @manage-membership-allowed?*))
     [:div.alert.alert-warning
      "This group is " [:strong "protected"] ". "
      "Memebership can be inspected but not changed!"])
   [filter-component]
   [table/toolbar]
   [table-component]
   [table/toolbar]
   [debug-component]
   [users/debug-component]])

(defn page []
  [:article.group.my-5
   [routing/hidden-state-component
    {:did-mount group-core/clean-and-fetch}]
   [group/header]
   [:section
    [group/properties-table]
    [group/edit-button]
    [group/delete-button]]

   [:section
    [:> Nav {:variant "tabs" :className "mt-5"
             :defaultActiveKey "users"}
     [:> Nav.Item
      [:> Nav.Link
       {:href (clojure.core/str
               "/admin/groups/"
               (-> @routing/state* :route-params :group-id))}
       "Inventory-Pools"]]
     [:> Nav.Item
      [:> Nav.Link
       {:active true}
       "Users"]]]
    [main-page-component]]])
