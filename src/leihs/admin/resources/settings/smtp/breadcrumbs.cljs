(ns leihs.admin.resources.settings.smtp.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.admin.resources.settings.icons :as icons]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.settings.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn smtp-settings-li []
  [li :smtp-settings
   [:span icons/smtp " SMTP "] {} {}
   :authorizers [auth/system-admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [smtp-settings-li])))
