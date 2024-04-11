(ns leihs.admin.resources.inventory-pools.breadcrumbs
  (:require [leihs.admin.common.icons :as icons]
            [leihs.admin.resources.breadcrumbs :as breadcrumbs]
            [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
            [leihs.core.auth.core :as auth]
            [reagent.core :as reagent :refer [reaction]]
            [taoensso.timbre]))

(def li breadcrumbs/li)

;;; Inventory Pools ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pools-li []
  [li :inventory-pools
   [:span [icons/inventory-pools] " Inventory-Pools "]
   {} {}
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [inventory-pools-li])))
