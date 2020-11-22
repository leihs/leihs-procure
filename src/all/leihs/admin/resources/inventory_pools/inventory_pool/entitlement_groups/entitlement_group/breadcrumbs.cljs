(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.breadcrumbs
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
    [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.breadcrumbs :as breadcrumbs-parent]
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
   :authorizers default-authorizers ])

(defn users-li []
  [li :inventory-pool-entitlement-group-users
   [:span icons/users "  Users"]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn groups-li []
  [li :inventory-pool-entitlement-group-groups
   [:span icons/groups " Groups"]
   @default-route-params* {}
   :authorizers default-authorizers])

(defonce left*
  (reaction
    (conj @breadcrumbs-parent/left*
          [entitlement-group-li])))
