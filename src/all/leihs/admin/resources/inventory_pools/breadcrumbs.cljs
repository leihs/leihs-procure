(ns leihs.admin.resources.inventory-pools.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.core.auth.core :as auth]
    [leihs.core.breadcrumbs :as core-breadcrumbs :refer [li]]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


;;; Inventory Pools ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pools-li []
  [li :inventory-pools
   [:span icons/inventory-pools " Inventory-Pools "]
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
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [inventory-pools-li]]))
