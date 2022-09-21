(ns leihs.admin.resources.buildings.building.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.resources.buildings.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.state :as state]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defonce building-id*
  (reaction (or (-> @routing/state* :route-params :building-id)
                "building-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn modifieable? [current-user-state _]
  (auth/admin-scopes? current-user-state _))

(defn edit-li []
  [breadcrumbs/li :building-edit [:span [icons/edit] " Edit "]
   {:building-id @building-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :building-delete [:span [icons/delete] " Delete "]
   {:building-id @building-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn building-li []
  [li :building [:span [icons/building] " Building "] {:building-id @building-id*} {}
   :authorizers [auth/admin-scopes?]])

(def buildings-li breadcrumbs/buildings-li)

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [building-li])))
