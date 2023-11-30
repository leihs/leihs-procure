(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def li breadcrumbs-parent/li)
(def nav-component breadcrumbs-parent/nav-component)
(def default-route-params* breadcrumbs-parent/default-route-params*)
(def default-authorizers breadcrumbs-parent/default-authorizers)

(defn create-li []
  [li :inventory-pool-delegation-create
   [:span [:i.fas.fa-plus-circle] " Create delegation "]
   @default-route-params* {}
   :button true
   :authorizers default-authorizers])

(defonce left*
  (reaction
   (conj @breadcrumbs-parent/left*
         [breadcrumbs-parent/delegations-li])))
