(ns leihs.admin.resources.system.authentication-systems.authentication-system.groups.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.breadcrumbs :as breadcrumbs]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]

   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)
(def authentication-system-id* breadcrumbs/authentication-system-id*)

(def group-id* (or (reaction (-> @routing/state* :route-params :group-id))
                   ":group-id"))

;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [breadcrumbs/groups-li])))
