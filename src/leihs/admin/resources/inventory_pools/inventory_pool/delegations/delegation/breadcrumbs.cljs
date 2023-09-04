(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
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

(def create-li breadcrumbs-parent/create-li)

(defn delegation-li []
  [li :inventory-pool-delegation
   [:span [icons/delegation] " Delegation "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn edit-li []
  [li :inventory-pool-delegation-edit
   [:span [:i.fas.fa-edit] " Edit "]
   @default-route-params* {:name (:name @delegation/delegation*)
                           :pool_protected (:pool_protected @delegation/delegation*)
                           :responsible_user_id (or (-> @delegation/delegation* :responsible_user :email)
                                                    (-> @delegation/delegation* :responsible_user :id))}
   :button true
   :authorizers default-authorizers])

(defn users-li []
  [li :inventory-pool-delegation-users
   [:span [icons/users] " Users "]
   @default-route-params* {}
   :authorizers default-authorizers])

(defn groups-li []
  [li :inventory-pool-delegation-groups
   [:span [icons/groups] " Groups"]
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
