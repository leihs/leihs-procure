(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.auth.core :as auth]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def inventory-pool-id* breadcrumbs-parent/inventory-pool-id*)

;(def users-li breadcrumbs-parent/users-li)

(defonce left*
  (reaction
    (conj @breadcrumbs-parent/left*
          [breadcrumbs-parent/groups-li])))
