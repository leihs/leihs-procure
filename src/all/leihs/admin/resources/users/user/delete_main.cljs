(ns leihs.admin.resources.users.user.delete-main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.resources.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as user-core :refer [user-id* user-data*]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

    [clojure.set :refer [rename-keys]]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]

    [taoensso.timbre :as logging]))


(def transfer-data* (reagent/atom {}))

(defn set-transfer-data-by-query-params [& _]
  (reset! transfer-data*
          (-> @routing/state*
              :query-params-raw
              (select-keys [:user-uid])
              (rename-keys {:user-uid :target-user-uid}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-user [& _]
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
     [:form.form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (delete-user))}
      [form-components/delete-submit-component]]]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transfer-data-and-delete-user [& _]
  (let [resp-chan (async/chan)
        url (path :user-transfer-data
                  {:user-id @user-id*
                   :target-user-uid (:target-user-uid @transfer-data*)})
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

(defn target-user-choose-component []
  [:div.input-group-append
   [:a.btn.btn-info
    {:tab-index 3
     :href (path :users-choose {}
                 {:return-to (path (:handler-key @routing/state*)
                                   (:route-params @routing/state*)
                                   @transfer-data*)})}
    [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
    " Choose user "]])

(defn delete-with-transfer-component []
  [:div.card.m-3
   [routing/hidden-state-component
    {:did-mount set-transfer-data-by-query-params}]
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
    [:form.form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (transfer-data-and-delete-user))}
     [form-components/input-component transfer-data* [:target-user-uid]
      :label "Target user"
      :placeholder "email-address, login, or id, or choose by clicking the button"
      :append target-user-choose-component]
     [form-components/submit-component
      :inner "Transfer and delete"
      :icon icons/delete]]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div.data
      [:h3 "transfer-data*"]
      [:pre (with-out-str (pprint @transfer-data*))]]
     [:div.data
      [:h3 "transfer-data*"]
      [:pre (with-out-str
              (pprint
                (path :user-transfer-data
                      {:user-id @user-id*
                       :target-user-uid (:target-user-uid @transfer-data*)})))]]]))

(defn page []
  [:div.user-delete
   [routing/hidden-state-component
    {:did-mount user-core/clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li] ) []]
   [:h1 "Delete User "
    (when-let [user @user-data*] (user-core/name-component user))
    " (" (when-let [user @user-data*] (user-core/some-uid user)) ") " ]
   [delete-without-reasignment-component]
   [delete-with-transfer-component]
   [debug-component]])
