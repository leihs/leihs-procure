(ns leihs.admin.resources.system.authentication-systems.authentication-system.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :as components]

   [leihs.admin.common.icons :as icons]
   [leihs.admin.common.membership.users.main :as users-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.resources.system.authentication-systems.authentication-system.users.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.system.authentication-systems.authentication-system.users.shared :refer [authentication-system-users-filter-value]]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.regex :as regex]

   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.shared :refer [short-id]]
   [reagent.core :as reagent]))

;;; path helpers  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path [user]
  (path :authentication-system-user
        {:authentication-system-id @authentication-system/id*
         :user-id (:id user)}))

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :authentication-system-groups
         (merge
          {:authentication-system-id @authentication-system/id*
           :user-id (:id user)}
          more-route-params)
         (merge
          {:including-user (or (-> user :email presence) (:id user))}
          more-query-params))))

(defn table-component []
  [users/table-component
   [users/user-th-component
    users-membership/member-user-th-component
    users-membership/direct-member-user-th-component
    users-membership/group-member-user-th-component]
   [users/user-td-component
    users-membership/member-user-td-component
    (users-membership/create-direct-member-user-td-component
     direct-member-path)
    (users-membership/create-group-member-user-td-component
     groups-path-fn)]])

(defn main-component []
  [:div
   [users-membership/filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [users/debug-component]])

(defn page []
  [:div.authentication-system-users
   [breadcrumbs/nav-component @breadcrumbs/left* []]
   [:div
    [:h1
     [:span " Users in the Authentication-System "]
     [authentication-system/name-component]]]
   [main-component]])
