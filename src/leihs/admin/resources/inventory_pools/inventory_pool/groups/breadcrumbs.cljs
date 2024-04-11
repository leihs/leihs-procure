(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs
  (:require [leihs.admin.common.breadcrumbs :as breadcrumbs]
            [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
            [reagent.core :as reagent :refer [reaction]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [breadcrumbs-parent/groups-li])))
