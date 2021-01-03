(ns leihs.admin.resources.audits.requests.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.resources.audits.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn requests-li []
  [li :audited-requests
   [:span audits/icon-requests " Audited-Requests " ] {} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [requests-li])))
