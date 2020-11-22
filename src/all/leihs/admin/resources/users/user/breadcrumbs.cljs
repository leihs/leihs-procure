(ns leihs.admin.resources.users.user.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :refer [user-data*]]
    [leihs.admin.resources.inventory-pools.authorization :refer [some-lending-manager?]]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(defn some-lending-manager-user-unprotected? [current-user-state _]
  (and (some-lending-manager? current-user-state _)
       (boolean  @user-data*)
       (-> @user-data* :protected not)))

(defn edit-li [id]
  [breadcrumbs/li :user-edit [:span [:i.fas.fa-edit] " Edit "]
   {:user-id id} {}
   :button true
   :authorizers [auth/admin-scopes?
                 some-lending-manager-user-unprotected?]])

(defn delete-li [id]
  [breadcrumbs/li :user-delete [:span [:i.fas.fa-times] " Delete "]
   {:user-id id} {}
   :button true
   :authorizers [auth/admin-scopes?
                 some-lending-manager-user-unprotected?]])

