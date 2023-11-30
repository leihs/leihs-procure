(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs :as breadcrumbs-parent]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

(defonce group-id*
  (reaction (or (some-> @routing/state* :route-params :group-id)
                ":group-id")))

(def route-params-default* (reaction
                            {:inventory-pool-id @inventory-pool-id*
                             :group-id @group-id*}))

(def authorizers-default [auth/admin-scopes? pool-auth/pool-lending-manager?])

(defn group-li []
  [li :inventory-pool-group [:span [icons/group] " Group "]
   @route-params-default* {}
   :link-disabled true
   :authorizers authorizers-default])

(defn group-roles-li []
  [li :inventory-pool-group-roles [:span [icons/edit] " Manage Group Roles "]
   @route-params-default* {}
   :authorizers authorizers-default])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [group-li])))
