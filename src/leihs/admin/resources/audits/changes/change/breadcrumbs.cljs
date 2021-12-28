(ns leihs.admin.resources.audits.changes.change.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.resources.audits.changes.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def audited-change-id*
  (reaction (or (-> @routing/state* :route-params :audited-change-id)
                ":audited-change-id")))

(defn change-li []
  [li :audited-change
   [:span audits/icon-change " Audited-Change " ]
   {:audited-change-id @audited-change-id*} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [change-li])))
