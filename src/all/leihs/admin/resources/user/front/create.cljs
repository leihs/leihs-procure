(ns leihs.admin.resources.user.front.create
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


(defn create [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :users)
                               :method :post
                               :json-params (-> @user-data*
                                                 (update-in
                                                   [:extended_info]
                                                   (fn [s] (.parse js/JSON s))))}
                              {:modal true
                               :title "Create User"
                               :handler-key :user-new
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :user {:user-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       " Create "]]
     [:div.clearfix]]))

(defn page []
  [:div.new-user
   [routing/hidden-state-component
    {:did-mount #(reset! user-data* {:account_enabled true
                                      :password_sign_in_enabled true})}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " New User "]]]]
   [user.shared/user-component create-submit-component create]
   [user.shared/debug-component]])
