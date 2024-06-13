(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.edit
  (:require [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
            [leihs.admin.resources.users.user.core :as core]
            [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
            [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
            [leihs.core.routing.front :as routing]))

(defn page []
  [:div.user-data
   [routing/hidden-state-component {:did-mount core/clean-and-fetch}]
   [:h1
    "Edit User "
    [core/name-link-component]
    " in the Inventory-Pool "
    [inventory-pool/name-link-component]]
   (when (not @data*)
     [wait-component])
   [edit-core/debug-component]])
