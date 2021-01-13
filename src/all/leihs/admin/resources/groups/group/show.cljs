(ns leihs.admin.resources.groups.group.show
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.group.breadcrumbs :as breadcrums]
    [leihs.admin.resources.groups.group.core :refer [group-id* data* debug-component edit-mode?* clean-and-fetch fetch-group group-name-component group-id-component]]
    [leihs.admin.resources.groups.group.inventory-pools :as inventory-pools] [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defn li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.m-2
    [:dt.col-sm-4 dt]
    [:dd.col-sm-8 dd]]])

(defn properties-component []
  [:div
   (if-not @data*
     [wait-component]
     [:ul.list-unstyled
      [li-dl-component "Name" (:name @data*)]
      [li-dl-component "Description " (:description @data*)]
      [li-dl-component "Protected" (if (:protected @data*)
                                     "yes"
                                     "no")]
      [li-dl-component "Org ID" (:org_id @data*)]
      [li-dl-component "Number of users" (:users_count @data*)]])])

(defn page []
  [:div.group
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrums/nav-component
    @breadcrums/left*
    [[breadcrums/users-li]
     [breadcrums/delete-li]
     [breadcrums/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Group "]
      [group-name-component]]
     [properties-component]
     [:div
      [:h2 "Inventory-Pools"]
      [inventory-pools/table-component]]
     [debug-component] ]]])
