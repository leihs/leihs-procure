(ns leihs.admin.resources.users.user.groups
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :as user-core :refer [user-id* user-data*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(defonce data* (reagent/atom nil))

(defn fetch-groups []
  (http/url-cached-fetch data*))

(defn debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.groups-debug
      [:hr]
      [:div.groups-data
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]])])

(defn group-td-component [row]
  [:td
   [:a {:href (path :group {:group-id (:group_id row)})}
    (:name row)]])


(defn table-component []
  [:div.user-groups
   [routing/hidden-state-component
    {:did-change fetch-groups}]
   (if (and @data* @user-data*)
     [:table.table.table-striped.table-sm.user-groups
      [:thead
       [:tr
        [:th "Group"]
        [:th.text-center "Org ID"]
        [:th.text-right "# Users"]]]
      [:tbody
       (for [row (->>  @data* (sort-by :inventory_pool_name))]
         [:tr.pool {:key (:group_id row)}
          [group-td-component row]
          [:td.text-center (:org_id row)]
          [:td.text-right (:users_count row)]])]]
     [wait-component])
   [debug-component]])

