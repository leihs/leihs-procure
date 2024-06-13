(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main
  (:require [cljs.core.async :as async :refer [go <!]]
            [cljs.pprint :refer [pprint]]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.common.roles.components :refer [put-roles<
                                                         roles-component]]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.groups.group.core :as group]
            [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
            [leihs.admin.state :as state]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent]))

(defonce changed?* (reagent/atom false))

(defonce data* (reagent/atom nil))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-inventory-pool-group-roles []
  (go (reset! data*
              (some->
               {:url (path :inventory-pool-group-roles
                           (-> @routing/state* :route-params))
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch []
  (reset! changed?* false)
  (reset! data* nil)
  (fetch-inventory-pool-group-roles)
  (group/clean-and-fetch)
  (inventory-pool/clean-and-fetch))

(defn header-component []
  [:h1 "Roles for the group "
   [group/group-name-component]
   " in the inventory-pool "
   [inventory-pool/name-link-component]])

(defn page []
  [:div.inventory-pool-group-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [header-component]
   [:div.form
    [roles-component @data*
     :update-handler #(go (reset! data*
                                  (<! (put-roles<
                                       (path :inventory-pool-group-roles
                                             (:route-params @routing/state*))
                                       %))))]]
   [debug-component]])
