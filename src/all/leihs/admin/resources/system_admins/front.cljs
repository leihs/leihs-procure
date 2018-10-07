(ns leihs.admin.resources.system-admins.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system-admins.breadcrumbs :as sa-breadcrumbs]
    [leihs.admin.resources.system-admins.shared :as shared]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.seq :refer [with-index]]

    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(def system-admin-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :system-admin_users_count)))


(def colconfig users/default-colconfig)


(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@system-admin-users-count*"
      [:pre (with-out-str (pprint @system-admin-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change users/escalate-query-paramas-update}]
   [filter-component]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn page []
  [:div.system-admin-users
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (sa-breadcrumbs/system-admins-li)]
     [(sa-breadcrumbs/system-admin-direct-users-li)
      (sa-breadcrumbs/system-admin-groups-li)])
   [:div
    [:h1
     (let [c (or @system-admin-users-count* 0)]
       [:span c " " (pluralize-noun c "System-Admin")])]
    [main-page-component]]])
