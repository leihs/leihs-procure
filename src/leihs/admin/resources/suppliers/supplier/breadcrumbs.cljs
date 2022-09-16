(ns leihs.admin.resources.suppliers.supplier.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.resources.suppliers.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    ; [leihs.admin.resources.suppliers.supplier.core :refer [supplier-data*]]
    [leihs.admin.state :as state]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defonce supplier-id*
  (reaction (or (-> @routing/state* :route-params :supplier-id)
                "supplier-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn modifieable? [current-user-state _]
  (auth/admin-scopes? current-user-state _))

(defn edit-li []
  [breadcrumbs/li :supplier-edit [:span [icons/edit] " Edit "]
   {:supplier-id @supplier-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :supplier-delete [:span [icons/delete] " Delete "]
   {:supplier-id @supplier-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn supplier-li []
  [li :supplier [:span [icons/suppliers] " Supplier "] {:supplier-id @supplier-id*} {}
   :authorizers [auth/admin-scopes?]])

(def suppliers-li breadcrumbs/suppliers-li)

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [supplier-li])))
