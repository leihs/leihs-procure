(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs
  (:require [leihs.admin.common.icons :as icons]
            [leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs :as breadcrumbs-parent]
            [leihs.core.core :refer [presence]]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]
            [taoensso.timbre]))

(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)

(defonce delegation-id*
  (reaction (or (-> @routing/state* :route-params :delegation-id presence)
                ":delegation-id")))

(def default-route-params*
  (reaction (merge @breadcrumbs-parent/default-route-params*
                   {:delegation-id @delegation-id*})))

(def default-authorizers breadcrumbs-parent/default-authorizers)

;;; Inventory Pool Delegation(s) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegation-li []
  [li :inventory-pool-delegation
   [:span [icons/delegation] " Delegation "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn suspension-li []
  [li :inventory-pool-delegation-suspension
   [:span [icons/edit] " Suspension"]
   @default-route-params* {}
   :button true
   :authorizers default-authorizers])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [delegation-li])))
