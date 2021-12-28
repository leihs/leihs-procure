(ns leihs.admin.resources.audits.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn audits-li []
  [li :audits
   [:span  audits/icon-audits " Audits "] {} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [audits-li])))
