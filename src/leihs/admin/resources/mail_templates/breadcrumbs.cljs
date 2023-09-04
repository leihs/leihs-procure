(ns leihs.admin.resources.mail-templates.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [split trim]]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.breadcrumbs :as breadcrumbs]
    [leihs.core.auth.core :as auth]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent :refer [reaction]]))

(def li breadcrumbs/li)
(def nav-component breadcrumbs/nav-component)

(defn mail-templates-li []
  [li :mail-templates [:span [icons/mail-templates] " Mail-Templates "] {} {}
   :authorizers [auth/admin-scopes?]])

(defn create-li []
  [li :mail-template-create
   [:span [:i.fas.fa-plus-circle] " Create Mail-Template "] {} {}
   :button true
   :authorizers [auth/admin-scopes?]])

;;; main ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [mail-templates-li])))
