(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent :refer [reaction]]))


(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

;(def users-li breadcrumbs-parent/users-li)

(defonce left*
  (reaction
    (conj @breadcrumbs-parent/left*
          [breadcrumbs-parent/groups-li])))
