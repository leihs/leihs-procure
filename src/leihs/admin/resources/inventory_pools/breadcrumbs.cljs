(ns leihs.admin.resources.inventory-pools.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

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
