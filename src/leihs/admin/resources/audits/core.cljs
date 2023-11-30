(ns leihs.admin.resources.audits.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [keyword str presence]]

   [leihs.core.routing.front :as routing]))

(def icon-audits [:i.fas.fa-history])
(def icon-changes [:i.far.fa-save])
(def icon-change [:i.far.fa-save])
(def icon-requests [:i.fas.fa-exchange-alt])
(def icon-request [:i.fas.fa-exchange-alt])

