(ns leihs.admin.resources.authentication-systems.breadcrumbs
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

(def authentication-system-id* (reaction (-> @routing/state* :route-params :authentication-system-id)))
(def user-id* (reaction (-> @routing/state* :route-params :user-id)))

(defn authentication-system-delete-li [] 
  (li :authentication-system-delete 
      [:span [:i.fas.fa-times] " Delete "] 
      {:authentication-system-id @authentication-system-id*} {}))

(defn authentication-system-edit-li [] 
  (li :authentication-system-edit 
      [:span [:i.fas.fa-edit] " Edit "] 
      {:authentication-system-id @authentication-system-id*} {}))

(defn authentication-system-li [] 
  (li :authentication-system 
      [:span icons/authentication-system " Authentication-System "] 
      {:authentication-system-id @authentication-system-id*} {}))

(defn authentication-system-add-li [] 
  (li :authentication-system-add 
      [:span [:i.fas.fa-plus-circle] " Add authentication-system "]))

(defn authentication-systems-li [] 
  (li :authentication-systems 
      [:span icons/authentication-systems " Authentication-Systems "] {} {}))

(defn authentication-system-users-li []
  (li :authentication-system-users
      [:span icons/users " Users "]
      {:authentication-system-id @authentication-system-id*} {}))

(defn authentication-system-user-add-li []
  (li :authentication-system-users-add
      [:span icons/add " Add user with data"]
      {:authentication-system-id @authentication-system-id*
       :user-id @user-id*}{}))

(defn authentication-system-user-edit-li []
  (li :authentication-system-users-edit
      [:span icons/edit " Edit user-data "]
      {:authentication-system-id @authentication-system-id*
       :user-id @user-id*}{}))
