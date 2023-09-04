(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main
  (:refer-clojure :exclude [str keyword])
  (:require
     [accountant.core :as accountant]
     [cljs.core.async :as async :refer [<! go]]
     [cljs.pprint :refer [pprint]]
     [leihs.admin.common.components :as components]
     [leihs.admin.common.form-components :as form-components]
     [leihs.admin.common.http-client.core :as http-client]
     [leihs.admin.common.icons :as icons]
     [leihs.admin.common.roles.components :refer [roles-component fetch-roles< put-roles<]]
     [leihs.admin.common.roles.core :as roles]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
     [leihs.admin.state :as state]
     [leihs.admin.utils.regex :as regex]
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.routing.front :as routing]
     [reagent.core :as reagent :refer [reaction]]))


(def roles-data* (reagent/atom nil))

(def roles-path* (reaction (path :inventory-pool-user-direct-roles
                                 (-> @routing/state* :route-params))))
(defn fetch [& _]
  (go (reset! roles-data* (<! (fetch-roles< @roles-path* )))))

(defn update-handler [new-roles]
  (let [chan (async/chan)]
    (go (let [new-roles  (<! (put-roles< @roles-path* new-roles))]
          (reset! roles-data* new-roles)
          (async/put! chan new-roles)))
    chan))

(defn header-component []
  [:h1 "Direct Roles for "
   [user/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn page []
  [:div.inventory-pool-user-direct-roles
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/direct-roles-li])[]]
   [header-component]
   [routing/hidden-state-component
    {:did-change fetch}]
   [roles-component @roles-data*
    :update-handler update-handler
    :compact false]])
