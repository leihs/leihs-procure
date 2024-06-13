(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main
  (:require [cljs.core.async :as async :refer [go <!]]
            [cljs.pprint :refer [pprint]]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.common.roles.components :as roles-ui :refer [put-roles<]]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
            [leihs.admin.resources.users.user.core :as user]
            [leihs.admin.state :as state]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent]))

(defonce roles-data* (reagent/atom nil))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@roles-data*"]
      [:pre (with-out-str (pprint @roles-data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-inventory-pool-user-roles []
  (go (reset! roles-data*
              (some->
               {:chan (async/chan)
                :url (path :inventory-pool-user-roles
                           (-> @routing/state* :route-params))}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch []
  (reset! roles-data* nil)
  (fetch-inventory-pool-user-roles)
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))

;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn header-component []
  [:h1 "Roles for "
   [user/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn remarks-component []
  [:div
   [:p "These roles represent an " [:strong  "aggregate"] " state derived from "
    [:strong " direct roles"] ", " " e.i. inventory-pool to user roles, and roles given "
    [:strong "through groups"] "."
    " The resource behind these roles is for " [:strong  " compatibility reasons writable"]
    " as long as the aggregate is backed soly by direct roles. "
    " The ability to write to this resource is " [:strong  "deprecated"]
    " and will be removed in future versions of leihs. "]])

(defn page []
  [:div.inventory-pool-user-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [header-component]
   [remarks-component]
   [roles-ui/roles-component @roles-data*
    :update-handler #(go (reset! roles-data*
                                 (<! (put-roles<
                                      (path :inventory-pool-user-roles
                                            (:route-params @routing/state*))
                                      %))))]
   [debug-component]])
