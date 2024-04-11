(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs
  (:require [leihs.admin.common.icons :as icons]
            [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
            [leihs.admin.resources.inventory-pools.inventory-pool.users.breadcrumbs :as breadcrumbs-parent]
            [leihs.core.auth.core :as auth]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]))

(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

(defonce user-id*
  (reaction (or (some-> @routing/state* :route-params :user-id)
                ":user-id")))

(def route-params-default* (reaction
                            {:inventory-pool-id @inventory-pool-id*
                             :user-id @user-id*}))

(def authorizers-default [auth/admin-scopes? pool-auth/pool-lending-manager?])

(defn edit-li []
  [li :inventory-pool-user-edit [:span [:i.fas.fa-edit] " User-Data "]
   {:inventory-pool-id @inventory-pool-id*
    :user-id @user-id*} {}
   :button true
   :authorizers [auth/admin-scopes? pool-auth/pool-lending-manager?]])

(defn user-li []
  [li :inventory-pool-user [:span [icons/user] " User "]
   @route-params-default* {}
   :authorizers authorizers-default])

(defn roles-li []
  [li :inventory-pool-user-roles [:span [icons/edit] " Manage roles "]
   @route-params-default* {}
   :authorizers authorizers-default])

(defn direct-roles-li []
  [li :inventory-pool-user-direct-roles
   [:span [icons/view] " " [icons/edit] " Direct roles "]
   @route-params-default* {}
   :button true
   :authorizers authorizers-default])

(defn suspension-li []
  [li :inventory-pool-user-suspension
   [:span [icons/view] " " [icons/edit] " Suspension"]
   @route-params-default* {}
   :button true
   :authorizers authorizers-default])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [user-li])))
