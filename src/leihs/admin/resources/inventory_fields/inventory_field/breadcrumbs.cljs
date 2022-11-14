(ns leihs.admin.resources.inventory-fields.inventory-field.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.resources.inventory-fields.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.state :as state]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defonce inventory-field-id*
  (reaction (or (-> @routing/state* :route-params :inventory-field-id)
                "inventory-field-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn modifieable? [current-user-state _]
  (auth/admin-scopes? current-user-state _))

(defn edit-li []
  [breadcrumbs/li :inventory-field-edit [:span [icons/edit] " Edit "]
   {:inventory-field-id @inventory-field-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :inventory-field-delete [:span [icons/delete] " Delete "]
   {:inventory-field-id @inventory-field-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn inventory-field-li []
  [li :inventory-field [:span [icons/inventory-field] " Inventory-Field "]
   {:inventory-field-id @inventory-field-id*} {}
   :authorizers [auth/admin-scopes?]])

(def inventory-fields-li breadcrumbs/inventory-fields-li)

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [inventory-field-li])))
