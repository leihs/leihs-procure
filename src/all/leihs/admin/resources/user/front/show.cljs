(ns leihs.admin.resources.user.front.show
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


(defn page []
  [:div.user
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [[breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/users-li]
      [breadcrumbs/user-li @user-id*]]
     [[breadcrumbs/user-delete-li @user-id*]
      [breadcrumbs/user-edit-li @user-id*]
      [breadcrumbs/email-li (:email @user-data*)]
      [breadcrumbs/user-my-li @user-id*]
      [breadcrumbs/user-inventory-pools-rooles-li @user-id*]])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " User "]
      [user.shared/user-name-component]]
     [user.shared/user-id-component]]]
   [user.shared/user-component nil]
   [user.shared/debug-component]])

