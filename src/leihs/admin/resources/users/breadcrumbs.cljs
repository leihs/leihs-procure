(ns leihs.admin.resources.users.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.resources.inventory-pools.authorization :refer [some-lending-manager?]]
    [leihs.admin.resources.users.user.core :refer [user-data*]]
    [leihs.admin.state :as state]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
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
