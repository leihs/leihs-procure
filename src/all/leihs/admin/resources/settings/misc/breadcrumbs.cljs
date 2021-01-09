(ns leihs.admin.resources.settings.misc.breadcrumbs
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

(defn misc-settings-li []
  [li :misc-settings
   [:span icons/misc " Miscellaneous "] {} {}
   :authorizers [auth/admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [misc-settings-li])))
