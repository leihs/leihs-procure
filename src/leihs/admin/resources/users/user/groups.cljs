(ns leihs.admin.resources.users.user.groups
  (:require [cljs.core.async :as async :refer [go <!]]
            [cljs.pprint :refer [pprint]]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.groups.main :as groups-core]
            [leihs.admin.resources.users.user.core :as user-core :refer [user-data*
                                                                         user-id*]]
            [leihs.admin.state :as state]
            [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent]))

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

(defn table-component []
  [:div.user-groups
   [routing/hidden-state-component
    {:did-change fetch-groups}]
   (if-not (and @data* @user-data*)
     [wait-component]
     [:<>
      [groups-core/core-table-component
       [groups-core/name-th-component
        groups-core/org-th-component
        groups-core/org-id-th-component
        groups-core/users-count-th-component]
       [groups-core/name-td-component
        groups-core/org-td-component
        groups-core/org-id-td-component
        groups-core/users-count-td-component]
       @data*]
      [debug-component]])])
