(ns leihs.admin.resources.groups.group.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.auth.core :as auth]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.group.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.groups.group.core :as group :refer [group-id* data*]]
    [leihs.admin.resources.groups.group.users.shared :refer [default-query-params]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.state :as state]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))




;### helpers ##################################################################

(def manage-membership-allowed*?
  (reaction
    (boolean
      (and @data*
           (some
             (fn [authorizer]
               (authorizer @current-user/state* @routing/state*))
             [auth/admin-scopes?
              breadcrumbs/some-lending-manager-group-unprotected?])))))


;### actions ##################################################################

(defn add-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :put
              :query-params {}}
             {:modal true
              :title "Add user"
              :handler-key :group-users
              :retry-fn #(add-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn remove-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :group-user {:group-id @group-id* :user-id user-id})
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove user"
              :handler-key :group-users
              :retry-fn #(remove-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn action-th-component []
  [:th.text-right "Add or remove from this group"])

(defn action-td-component [user]
  [:td.text-right
   (if (:group_id user)
     [:button.btn.btn-sm.btn-warning
      {:on-click (fn [_] (remove-user (:id user)))
       :disabled (not @manage-membership-allowed*?)}
      icons/delete " Remove "]
     [:button.btn.btn-sm.btn-primary
      {:on-click (fn [_] (add-user (:id user)))
       :disabled (not @manage-membership-allowed*?)}
      icons/add " Add "])])


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
    [:div ]))

(defn main-page-component []
  [:div
   (when (and @group/data*
              (not @manage-membership-allowed*?))
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
