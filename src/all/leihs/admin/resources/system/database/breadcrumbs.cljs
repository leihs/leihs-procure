(ns leihs.admin.resources.system.database.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.paths :as paths :refer [path]]))

(def li core-breadcrumbs/li)


(defn database-li []
  (li :database
      [:span [:i.fas.fa-database] " Database "]))


(defn database-audits-li []
  (li :database-audits
      [:span [:i.fas.fa-eye] " Audits "]))

(defn database-audits-before-li [date]
  (li :database-audits-before
      [:span [:i.fas.fa-less-than] " Before "]
      {:before-date date} {}))
