(ns leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.auth.core :as auth]
    ;[leihs.core.breadcrumbs :as breadcrumbs-core :refer [li]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defonce inventory-pool-id*
  (reaction (or (-> @routing/state* :route-params :inventory-pool-id presence)
                ":inventory-pool-id")))

(def default-authorizers [auth/admin-scopes?
                          pool-auth/pool-lending-manager?])

(def default-route-params* (reaction {:inventory-pool-id @inventory-pool-id*}))

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def create-li breadcrumbs-parent/create-li)

(defn delegations-li []
  [li :inventory-pool-delegations
   [:span icons/delegations " Delegations "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn entitlement-groups-li []
  [li :inventory-pool-entitlement-groups
   [:span icons/entitlement-groups " Entitlement-Groups "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn groups-li []
  [li :inventory-pool-groups [:span icons/groups " Groups "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn inventory-pool-li []
  [li :inventory-pool [:span icons/inventory-pool " Inventory-Pool "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn delete-li []
  [li :inventory-pool-delete
   [:span [:i.fas.fa-times] " Delete "]
   @default-route-params* {}
   :button true
   :authorizers [auth/admin-scopes?]])

(defn edit-li []
  [li :inventory-pool-edit
   [:span [:i.fas.fa-edit] " Edit "]
   @default-route-params* {}
   :button true
   :authorizers [auth/admin-scopes?
                 pool-auth/pool-inventory-manager?]])

(defn users-li []
  [li :inventory-pool-users [:span icons/users " Users "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defonce left*
  (reaction
    (conj @breadcrumbs-parent/left*
          [inventory-pool-li])))
