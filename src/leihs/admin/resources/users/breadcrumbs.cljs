(ns leihs.admin.resources.users.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.resources.inventory-pools.authorization :refer [some-lending-manager?]]
    [leihs.admin.resources.users.user.core :refer [user-data*]]
    [leihs.admin.state :as state]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent :refer [reaction]]
    ))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn user-create-li []
  [li :user-create [:span [:i.fas.fa-plus-circle] " Create user "] {} {}
   :button true
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn users-choose-li []
  [li :users-choose [:span  " Choose user "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn users-li []
  [li :users [:span [icons/users] " Users "] {} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [users-li])))
