(ns leihs.admin.resources.audits.requests.request.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.common.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.resources.audits.requests.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(def audited-request-id*
  (reaction (or (-> @routing/state* :route-params :audited-request-id)
                ":audited-request-id")))

(defn request-li []
  [li :audited-request
   [:span audits/icon-request " Audited-Reuest" ]
   {:audited-request-id @audited-request-id*} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [request-li])))
