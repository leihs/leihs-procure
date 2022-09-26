(ns leihs.admin.resources.rooms.room.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.resources.rooms.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.state :as state]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defonce room-id*
  (reaction (or (-> @routing/state* :route-params :room-id)
                "room-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn modifieable? [current-user-state _]
  (auth/admin-scopes? current-user-state _))

(defn edit-li []
  [breadcrumbs/li :room-edit [:span [icons/edit] " Edit "]
   {:room-id @room-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :room-delete [:span [icons/delete] " Delete "]
   {:room-id @room-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn room-li []
  [li :room [:span [icons/rooms] " Room "] {:room-id @room-id*} {}
   :authorizers [auth/admin-scopes?]])

(def rooms-li breadcrumbs/rooms-li)

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [room-li])))
