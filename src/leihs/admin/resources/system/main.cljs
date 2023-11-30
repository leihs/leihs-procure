(ns leihs.admin.resources.system.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async]
   [cljs.core.async :refer [timeout]]

   [cljs.pprint :refer [pprint]]
   [clojure.contrib.inflect :refer [pluralize-noun]]
   [leihs.admin.common.components :as components]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.breadcrumbs :as breadcrumbs]

   [leihs.admin.state :as state]
   [leihs.admin.utils.seq :refer [with-index]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defn breadcrumbs []
  [breadcrumbs/nav-component
   @breadcrumbs/left*
   [[breadcrumbs/authentication-systems-li]]])

(defn page []
  [:div.system
   [breadcrumbs]
   [:div
    [:h1 " System "]]])
