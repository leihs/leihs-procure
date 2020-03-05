(ns leihs.admin.resources.user.front.remove
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.user.front.shared :as user.shared :refer [clean-and-fetch user-id* user-data* edit-mode?*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(def transfer-data* (reagent/atom {}))

(defn delete-user [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user (-> @routing/state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete User"
                               :handler-key :user-delete
                               :retry-fn #'delete-user}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-without-reasignment-component []
  [:div.card.m-3
   [:div.card-header.bg-warning
    [:h2 "Delete User"]]
   [:div.card-body
    [:p
     "Deleting this user is not possible if it is associated with contracts, reserverations, or orders. "
     "If this is the case this operation will fail without deleting or even changing any data. "]
    [:p.text-danger
     "Permissions, such as given by delegations, groups, or roles will not prevent deletion of this user. " ]
    [:div.float-right
     [:button.btn.btn-warning.btn-lg
      {:on-click delete-user}
      [:i.fas.fa-times] " Delete"]]]])

(defn transfer-data-and-delete-user [_]
  (let [resp-chan (async/chan)
        url (path :user-transfer-data
                  {:user-id @user-id*
                   :target-user-id (:target-user-id @transfer-data*)})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:title "Transfer Data and Delete User"
              :handler-key :user-delete
              :retry-fn #'transfer-data-and-delete-user}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :users {}
                    (-> @state/global-state* :users-query-params))))))))

(defn delete-with-transfer-component []
  [:div.card.m-3
   [:div.card-header.bg-danger
    [:h2 "Transfer Data and Delete User"]]
   [:div.card-body
    [:p
     "Contracts, reserverations, and orders of this user will be "
     "transferred to the user entered below. " ]
    [:p.text-danger
     "Permissions, such as given by delegations, groups, or roles will not be
     transferred! " ]
    [:p.text-danger
     "Audits will still contain references to the removed user! "]
    [:p.text-danger
     "External data, such as open contracts printed on paper for example, "
     "will become inconsistent with the data in leihs!"]
    [:div.form
     [:div.form-group
      [:label {:for :target-user-id} "Target user id" ]
      [:input#target-user-id.form-control
       {:type :text
        :value (or (-> @transfer-data* :target-user-id) "")
        :on-change #(swap! transfer-data* assoc
                           :target-user-id (-> % .-target .-value presence))}]]]
    [:div.float-right
     [:button.btn.btn-danger
      {:on-click transfer-data-and-delete-user}
      [:i.fas.fa-times] " Transfer and delete"]]]])

(defn page []
  [:div.user-delete
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/users-li]
      [breadcrumbs/user-li @user-id*]
      [breadcrumbs/user-delete-li @user-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete User "
    [user.shared/user-name-component]]
   [user.shared/user-id-component]
   [delete-without-reasignment-component]
   [delete-with-transfer-component]])
