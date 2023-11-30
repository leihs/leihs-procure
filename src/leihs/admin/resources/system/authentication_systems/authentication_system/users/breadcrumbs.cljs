(ns leihs.admin.resources.system.authentication-systems.authentication-system.users.breadcrumbs
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

(def user-id* (or (reaction (-> @routing/state* :route-params :user-id))
                  ":user-id"))

(defn edit-li []
  [li :authentication-system-users-edit
   [:span [icons/edit] " Edit user-data "]
   {:authentication-system-id @authentication-system-id*
    :user-id @user-id*} {}
   :authorizers [auth/system-admin-scopes?]])

(defn user-li []
  [li :authentication-system-user
   [:span [icons/user] " User "]
   {:authentication-system-id @authentication-system-id*
    :user-id @user-id*} {}
   :authorizers [auth/system-admin-scopes?]])

;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [breadcrumbs/users-li])))
