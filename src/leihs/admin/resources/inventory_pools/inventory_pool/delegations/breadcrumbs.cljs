(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs
  (:require [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
            [reagent.core :as reagent :refer [reaction]]
            [taoensso.timbre]))

(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)
(def default-route-params* breadcrumbs-parent/default-route-params*)
(def default-authorizers breadcrumbs-parent/default-authorizers)

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [breadcrumbs-parent/delegations-li])))
