(ns leihs.admin.resources.users.user.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.resources.users.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :refer [user-data*]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

(defonce user-id*
  (reaction (or (-> @routing/state* :route-params :user-id)
                "user-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn some-lending-manager-user-unprotected? [current-user-state _]
  (and (pool-auth/some-lending-manager? current-user-state _)
       (boolean  @user-data*)
       (-> @user-data* :admin_protected not)))

(defn modifieable? [current-user-state _]
  (cond
    (auth/system-admin-scopes?
      current-user-state _) true
    (auth/admin-scopes?
      current-user-state
      _)  (cond (or (nil? @user-data*) (:is_system_admin @user-data*)) false
                (or (nil? @user-data*) (:system_admin_protected @user-data*)) false
                :else true )
    :else (cond (or (nil? @user-data*) (:is_admin @user-data*)) false
                (or (nil? user-data*) (:admin_protected @user-data*)) false
                :else true)))

(defn edit-li []
  [breadcrumbs/li :user-edit [:span [:i.fas.fa-edit] " Edit "]
   {:user-id @user-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :user-delete [:span [:i.fas.fa-times] " Delete "]
   {:user-id @user-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn user-li []
  [li :user [:span [icons/user] " User "] {:user-id @user-id*} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn user-my-li [user-id]
  [li :my-user [:span [icons/user]
                " User-Home in leihs/my
                " [:i.fas.fa-external-link-alt]]
   {:user-id user-id} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [user-li])))
