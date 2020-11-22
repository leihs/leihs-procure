(ns leihs.admin.resources.system.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.system.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.seq :refer [with-index]]

    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defn breadcrumbs []
  [breadcrumbs/nav-component
   @breadcrumbs/left*
   [[breadcrumbs/authentication-systems-li]
    [breadcrumbs/system-admins-li]]])

(defn page []
  [:div.system
   [breadcrumbs]
   [:div
    [:h1 " System " ]]])
