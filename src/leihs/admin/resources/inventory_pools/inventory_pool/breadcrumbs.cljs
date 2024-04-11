(ns leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs
  (:require [leihs.admin.common.breadcrumbs :as breadcrumbs]
            [leihs.admin.common.icons :as icons]
            [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
            [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs-parent]
            [leihs.core.auth.core :as auth]
            [leihs.core.core :refer [presence]]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]
            [taoensso.timbre]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defonce inventory-pool-id*
  (reaction (or (-> @routing/state* :route-params :inventory-pool-id presence)
                ":inventory-pool-id")))

(def default-authorizers [auth/admin-scopes?
                          pool-auth/pool-lending-manager?])

(def default-route-params* (reaction {:inventory-pool-id @inventory-pool-id*}))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegations-li []
  [li :inventory-pool-delegations
   [:span [icons/delegations] " Delegations "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn groups-li []
  [li :inventory-pool-groups [:span [icons/groups] " Groups "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn inventory-pool-li []
  [li :inventory-pool [:span [icons/inventory-pool] " Inventory-Pool "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn users-li []
  [li :inventory-pool-users [:span [icons/users] " Users "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [inventory-pool-li])))
