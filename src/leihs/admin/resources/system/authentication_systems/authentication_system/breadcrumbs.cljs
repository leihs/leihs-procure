(ns leihs.admin.resources.system.authentication-systems.authentication-system.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.breadcrumbs :as breadcrumbs]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]

   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def authentication-system-id*
  (reaction (or (-> @routing/state* :route-params :authentication-system-id presence)
                ":authentication-system-id")))

(def user-id* (reaction (-> @routing/state* :route-params :user-id)))
(def group-id* (reaction (-> @routing/state* :route-params :group-id)))

(defn delete-li []
  [li :authentication-system-delete
   [:span [:i.fas.fa-times] " Delete "]
   {:authentication-system-id @authentication-system-id*} {}
   :button true
   :authorizers [auth/system-admin-scopes?]])

(defn edit-li []
  [li :authentication-system-edit
   [:span [:i.fas.fa-edit] " Edit "]
   {:authentication-system-id @authentication-system-id*} {}
   :button true
   :authorizers [auth/system-admin-scopes?]])

(defn groups-li []
  [li :authentication-system-groups
   [:span [icons/groups] " Groups "]
   {:authentication-system-id @authentication-system-id*} {}
   :authorizers [auth/system-admin-scopes?]])

(defn users-li []
  [li :authentication-system-users
   [:span [icons/users] " Users "]
   {:authentication-system-id @authentication-system-id*} {}
   :authorizers [auth/system-admin-scopes?]])

(defn authentication-system-li []
  [li :authentication-system
   [:span [icons/authentication-system] " Authentication-System "]
   {:authentication-system-id @authentication-system-id*} {}
   :authorizers [auth/system-admin-scopes?]])

;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [authentication-system-li])))
