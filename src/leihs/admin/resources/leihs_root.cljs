(ns leihs.admin.resources.leihs-root
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [cljs.pprint :refer [pprint]]

   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.breadcrumbs :as breadcrumbs]
   [leihs.admin.state :as state]

   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as core-user]
   [reagent.core :as reagent]))

(defn page []
  [:div.home
   (when-let [user @core-user/state*]
     (breadcrumbs/nav-component
      [[breadcrumbs/leihs-li]]
      [[breadcrumbs/admin-li]
       [breadcrumbs/borrow-li]
       [breadcrumbs/lending-li]
       [breadcrumbs/procurement-li]]))
   [:h1 "leihs-admin Home"]
   [:p.text-danger "This page is only accessible for development and testing."]])
