(ns leihs.admin.resources.inventory.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.paths :as paths :refer [path]]))

(def li core-breadcrumbs/li)

(defn inventory-li [] (li :inventory [:span icons/inventory " Inventory "]))
