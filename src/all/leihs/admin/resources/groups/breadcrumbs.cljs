(ns leihs.admin.resources.groups.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))


(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn groups-li []
  [li :groups [:span icons/groups " Groups "] {} {}
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
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [groups-li]]))
