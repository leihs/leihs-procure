(ns leihs.admin.resources.groups.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn groups-li []
  [li :groups [:span [icons/groups] " Groups "] {} {}
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])

(defn create-li []
  [li :group-create
   [:span [:i.fas.fa-plus-circle] " Create group "] {} {}
   :button true
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [groups-li])))
