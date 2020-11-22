(ns leihs.admin.resources.system.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn authentication-systems-li []
  [li :authentication-systems
      [:span icons/authentication-systems " Authentication-Systems "] {} {}
      :authorizers [auth/admin-scopes?]])

(defn system-admins-li []
  [li :system-admins
      [:span icons/system-admin " System-Admins "] {} {}
      :authorizers [auth/system-admin-scopes?]])

(defn system-li []
  [li :system
      [:span icons/system " System "] {} {}
      :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [system-li]]))
