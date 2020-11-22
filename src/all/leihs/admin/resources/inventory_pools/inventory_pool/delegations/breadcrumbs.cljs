(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.auth.core :as auth]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


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
