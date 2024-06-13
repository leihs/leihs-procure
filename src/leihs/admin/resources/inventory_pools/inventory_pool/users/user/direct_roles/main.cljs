(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main
  (:require [cljs.core.async :as async :refer [<! go]]
            [leihs.admin.common.roles.components :refer [fetch-roles<
                                                         put-roles<
                                                         roles-component]]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
            [leihs.admin.resources.users.user.core :as user]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]))

(def roles-data* (reagent/atom nil))

(def roles-path* (reaction (path :inventory-pool-user-direct-roles
                                 (-> @routing/state* :route-params))))
(defn fetch [& _]
  (go (reset! roles-data* (<! (fetch-roles< @roles-path*)))))

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
   [header-component]
   [routing/hidden-state-component
    {:did-change fetch}]
   [roles-component @roles-data*
    :update-handler update-handler
    :compact false]])
