(ns leihs.admin.resources.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn admin-li [] [li :admin [:span [icons/admin] " Admin "] {} {}
                   :authorizers [auth/all-granted]])
(defn auth-li [] [li :auth "Authentication"])
(defn auth-info-li [] [li :auth-info "Info"])
(defn auth-password-sign-in-li [] [li :password-authentication "Password sign-in"])
(defn borrow-li [] [li :borrow "Borrow"])
(defn debug-li [] [li :debug "Debug"])

(defn groups-li []
  [li :groups [:span [icons/groups] " Groups "] {} {}
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])

(defn buildings-li []
  [li :buildings [:span [icons/building] " Buildings "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn inventory-fields-li []
  [li :inventory-fields [:span [icons/inventory-fields] " Inventory-Fields "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn mail-templates-li []
  [li :mail-templates [:span [icons/mail-templates] " Mail-Templates "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn rooms-li []
  [li :rooms [:span [icons/rooms] " Rooms "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn suppliers-li []
  [li :suppliers [:span [icons/suppliers] " Suppliers "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn leihs-li [] [li :home [:span [icons/home] " Home "] {} {}
                   :authorizers [auth/all-granted]])
(defn lending-li [] [li :lending "Lending"])
(defn procurement-li [] [li :procurement "Procurement"])
(defn requests-li [] [li :requests "Requests"])

(def left*
  (reaction
   [[leihs-li]
    [admin-li]]))
