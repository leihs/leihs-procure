(ns leihs.admin.resources.users.user.groups
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.main :as groups-core]
    [leihs.admin.resources.users.user.core :as user-core :refer [user-id* user-data*]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

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
  (go (reset! data*
              (-> {:chan (async/chan)
                   :url (path :groups {} {:including-user @user-id*
                                          :page 1
                                          :pre-page 1000})}
                  http-client/request
                  :chan <! http-client/filter-success! :body :groups))))

(defn debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.groups-debug
      [:hr]
      [:div.groups-data
       [:h3 "Groups @data*"]
       [:pre (with-out-str (pprint @data*))]]])])

(defn group-td-component [row]
  [:td
   [:a {:href (path :group {:group-id (:group_id row)})}
    (:name row)]])


(defn table-component []
  [:div.user-groups
   [routing/hidden-state-component
    {:did-change fetch-groups}]
   (if-not (and @data* @user-data*)
     [wait-component]
     [groups-core/core-table-component [] [] @data*])
   [debug-component]])

