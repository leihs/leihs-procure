(ns leihs.admin.resources.system.authentication-systems.authentication-system.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.membership.groups.main :as groups-membership]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups]
    [leihs.admin.resources.system.authentication-systems.authentication-system.groups.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.system.authentication-systems.authentication-system.main :as authentication-system]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


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

(defn main-page-component []
  [:div
   [groups-membership/filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [groups/debug-component]])

(defn page []
  [:div.authentication-system-groups
   [breadcrumbs/nav-component @breadcrumbs/left* []]
   [:div
    [:h1
     [:span " Groups in the Authentication-System "]
     [authentication-system/name-component]]
    [main-page-component]]])
