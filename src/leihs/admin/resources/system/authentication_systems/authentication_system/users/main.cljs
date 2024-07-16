(ns leihs.admin.resources.system.authentication-systems.authentication-system.users.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.membership.users.main :as users-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as auth-core]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]))

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
  [users/users-table
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

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(auth-core/fetch)
     :did-change #(users/fetch-users)}]

   (if-not @auth-core/data*
     [:div.my-5
      [wait-component]]
     [:article.authentication-system.my-5
      [auth-core/header]
      [:section
       [auth-core/tabs "users"]
       [users-membership/filter-component]
       [table/toolbar]
       [table-component]
       [table/toolbar]
       [users/debug-component]]])])
