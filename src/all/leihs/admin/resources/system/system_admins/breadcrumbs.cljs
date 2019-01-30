(ns leihs.admin.resources.system.system-admins.breadcrumbs
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

(def system-admin-id* (reaction (-> @routing/state* :route-params :system-admin-id)))
(def user-id* (reaction (-> @routing/state* :route-params :user-id)))

(defn system-admins-li []
  (li :system-admins
      [:span icons/system-admin " System-Admins "]{}{}))

(defn system-admin-direct-users-li []
  (li :system-admin-direct-users
      [:span icons/users " Direct-Users "]
      {} {}))

(defn system-admin-groups-li []
  (li :system-admin-groups
      [:span icons/groups " Groups "]
      {}{}))

