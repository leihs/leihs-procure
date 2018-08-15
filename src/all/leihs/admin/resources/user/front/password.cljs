(ns leihs.admin.resources.user.front.password
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.resources.user.front.edit :as user.edit]
    [leihs.admin.resources.user.front.shared :as user.shared :refer [user-id* user-data* edit-mode?*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defn patch [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user {:user-id @user-id*})
                               :method :patch
                               :json-params (select-keys @user-data* [:password])}
                              {:modal true
                               :title "Update User"
                               :handler-key :user-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :user {:user-id @user-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-warning
       [:i.fas.fa-save]
       " Set password "]]
     [:div.clearfix]]))

(defn page []
  [:div.edit-user
   [state/hidden-routing-state-component
    {:will-mount user.shared/clean-and-fetch
     :did-change user.shared/clean-and-fetch
     }]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user.shared/user-id*)
      (breadcrumbs/user-password-li @user.shared/user-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Set the Password for User "]
      [user.shared/user-name-component]]
     [user.shared/user-id-component]]]
   [:form.form.mt-2
    {:on-submit (fn [e]
                  (.preventDefault e)
                  (patch))}
    [:div {:style {:display :none}}
     [user.shared/field-component :email {:type :email}]]
    [user.shared/field-component :password {:type :password :autoComplete :new-password}]
    [patch-submit-component]]
   [user.shared/debug-component]])

