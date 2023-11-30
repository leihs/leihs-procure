(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.breadcrumbs :as breadcrumbs-parent]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

(def entitlement-group-id*
  (reaction (or (-> @routing/state* :route-params :entitlement-group-id)
                ":entitlement-group-id")))

(def default-route-params*
  (reaction
   {:inventory-pool-id @inventory-pool-id*
    :entitlement-group-id @entitlement-group-id*}))

(def default-authorizers [auth/admin-scopes?
                          pool-auth/pool-lending-manager?])

;;; Inventory Pool Entitlement-Group(s) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn entitlement-group-li []
  [li :inventory-pool-entitlement-group
   [:span " Entitlement-Group "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn users-li []
  [li :inventory-pool-entitlement-group-users
   [:span [icons/users] "  Users"]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn groups-li []
  [li :inventory-pool-entitlement-group-groups
   [:span [icons/groups] " Groups"]
   @default-route-params* {}
   :authorizers default-authorizers])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [entitlement-group-li])))
