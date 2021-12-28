(ns leihs.admin.resources.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.auth.core :as auth]

    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))

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

(defn leihs-li [] [li :home [:span [icons/home] " Home "] {} {}
                   :authorizers [auth/all-granted]])
(defn lending-li [] [li :lending "Lending"])
(defn procurement-li [] [li :procurement "Procurement"])
(defn requests-li [] [li :requests "Requests"])


(def left*
  (reaction
    [[leihs-li]
     [admin-li]]))
