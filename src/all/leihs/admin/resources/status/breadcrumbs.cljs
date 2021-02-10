(ns leihs.admin.resources.status.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.user.core :refer [user-data*]]
    [leihs.admin.state :as state]

    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn status-li []
  [li :status [:span [:i.fas.fa-thermometer-half] " Status-Info "] {} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [status-li])))
