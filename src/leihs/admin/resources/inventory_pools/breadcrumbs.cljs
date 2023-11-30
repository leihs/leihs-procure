(ns leihs.admin.resources.inventory-pools.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

;;; Inventory Pools ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pools-li []
  [li :inventory-pools
   [:span [icons/inventory-pools] " Inventory-Pools "]
   {} {}
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])

(defn create-li []
  [li :inventory-pool-create
   [:span [:i.fas.fa-plus-circle] " Create Inventory-Pool "]
   {} {}
   :button true
   :authorizers [auth/admin-scopes?]])

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [inventory-pools-li])))
