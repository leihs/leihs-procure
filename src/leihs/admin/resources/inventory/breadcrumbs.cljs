(ns leihs.admin.resources.inventory.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]


    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn inventory-li []
  [li :inventory [:span [icons/inventory] " Inventory "] {} {}
   :authorizers [auth/admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [inventory-li])))
