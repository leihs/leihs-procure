(ns leihs.admin.resources.buildings.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.breadcrumbs :as breadcrumbs]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn buildings-li []
  [li :buildings [:span [icons/building] " Buildings "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn create-li []
  [li :building-create
   [:span [:i.fas.fa-plus-circle] " Create Building "] {} {}
   :button true
   :authorizers [auth/admin-scopes?]])

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [buildings-li])))
