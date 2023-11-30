(ns leihs.admin.resources.mail-templates.mail-template.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [split trim]]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.mail-templates.breadcrumbs :as breadcrumbs]
   [leihs.admin.state :as state]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]
   [taoensso.timbre :refer [error warn info debug spy]]))

(defonce mail-template-id*
  (reaction (or (-> @routing/state* :route-params :mail-template-id)
                "mail-template-id")))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn modifieable? [current-user-state _]
  (auth/admin-scopes? current-user-state _))

(defn edit-li []
  [breadcrumbs/li :mail-template-edit [:span [icons/edit] " Edit "]
   {:mail-template-id @mail-template-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn delete-li []
  [breadcrumbs/li :mail-template-delete [:span [icons/delete] " Delete "]
   {:mail-template-id @mail-template-id*} {}
   :button true
   :authorizers [modifieable?]])

(defn mail-template-li []
  [li :mail-template [:span [icons/mail-template] " Mail-Template "] {:mail-template-id @mail-template-id*} {}
   :authorizers [auth/admin-scopes?]])

(def mail-templates-li breadcrumbs/mail-templates-li)

(defonce left*
  (reaction
   (conj @breadcrumbs/left* [mail-template-li])))
