(ns leihs.admin.resources.system.system-admins.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.system.breadcrumbs :as breadcrumbs]

    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.system-admins.shared :as shared]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.utils.seq :refer [with-index]]


    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(def system-admin-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :system-admin_users_count)))

(defn form-is-system-admin-filter []
  (let [is-system-admin (->> @users/current-query-paramerters-normalized*
                             (merge shared/default-query-params)
                             :is-system-admin)]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-is-system-admin} "Is system admin"]
     [:select#users-filter-is-system-admin.form-control
      {:value is-system-admin
       :on-change (fn [e]
                    (let [val (-> e .-target .-value presence)]
                      (accountant/navigate!
                        (users/page-path-for-query-params
                                              {:page 1
                                               :is-system-admin val}))))}
      (for [[k n] {"any" "any"
                   "yes" "yes"
                   "no" "no"}]
        [:option {:key k :value k} n])]]))

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [users/form-term-filter]
    [form-is-system-admin-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     ]))

(defn action-th-component []
   [:th.action "Action"])

(defn request [method user]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :system-admin
                         {:user-id (:id user)} {})
              :method method}
             {:modal true
              :title (str method " System Admin")}
             :chan resp-chan)]
    (go (<! resp-chan)
        (users/fetch-users))))

(defn action-td-component [user]
  [:td
   (if (:is_system_admin user)
     [:form
      {:on-submit (fn [e] (.preventDefault e) (request :delete user))}
      [form-components/small-remove-submit-component]]
     [:form
      {:on-submit (fn [e] (.preventDefault e) (request :put user))}
      [form-components/small-add-submit-component]])])

(defn table-component []
  [users/table-component
   [users/user-th-component
    action-th-component]
   [users/user-td-component
    action-td-component]])

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change users/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn breadcrumbs []
  [breadcrumbs/nav-component
   (conj @breadcrumbs/left*
         [breadcrumbs/system-admins-li])[]])

(defn page []
  [:div.system-admin-users
   [breadcrumbs]
   [:div
    [:h1 "System-Admins"]
    [main-page-component]]])
