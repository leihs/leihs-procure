(ns leihs.admin.resources.statistics.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
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

(defn statistics-li []
  [li :statistics
   [:span [:i.fas.fa-chart-line] " Statistics "] {} {}
   :authorizers [auth/admin-scopes?]])

(defonce left*
  (reaction
    (conj @breadcrumbs/left* [statistics-li])))
