(ns leihs.admin.resources.system.authentication-systems.authentication-system.groups.main
  (:require
   [leihs.admin.common.membership.groups.main :as groups-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as auth-core]
   [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn member-path
  ([group]
   (member-path group {}))
  ([group query-params]
   (path :authentication-system-group
         {:authentication-system-id @authentication-system/id*
          :group-id (:id group)} query-params)))

(defn table-component []
  [:div
   [routing/hidden-state-component
    {:did-change groups/fetch-groups}]
   [groups/table-component
    [groups/name-th-component
     groups-membership/member-th-component]
    [groups/name-td-component
     (partial groups-membership/member-td-component member-path)]]])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(auth-core/fetch)}]

   (if-not @auth-core/data*
     [:div.my-5
      [wait-component]]
     [:article.authentication-system.my-5
      [auth-core/header]
      [:section
       [auth-core/tabs "groups"]
       [groups-membership/filter-component]
       [table-component]
       [groups/debug-component]]])])
