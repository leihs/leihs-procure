(ns leihs.admin.resources.breadcrumbs
  (:require [leihs.admin.common.breadcrumbs :as breadcrumbs]
            [leihs.admin.common.icons :as icons]
            [leihs.core.auth.core :as auth]
            [reagent.core :as reagent :refer [reaction]]
            [taoensso.timbre]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)
(defn admin-li [] [li :admin [:span [icons/admin] " Admin "] {} {}
                   :authorizers [auth/all-granted]])
(defn borrow-li [] [li :borrow "Borrow"])
(defn leihs-li [] [li :home [:span [icons/home] " Home "] {} {}
                   :authorizers [auth/all-granted]])
(defn lending-li [] [li :lending "Lending"])
(defn procurement-li [] [li :procurement "Procurement"])

(def left*
  (reaction
   [[leihs-li]
    [admin-li]]))
