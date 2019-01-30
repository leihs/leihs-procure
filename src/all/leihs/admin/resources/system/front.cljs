(ns leihs.admin.resources.system.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as authentication-systems.breadcrumbs]
    [leihs.admin.resources.system.breadcrumbs :as system-breadcrumbs]
    [leihs.admin.resources.system.system-admins.breadcrumbs :as system-admins-breadcrumbs]
    [leihs.admin.utils.seq :refer [with-index]]

    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defn page []
  [:div.system
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      ]
     [(system-admins-breadcrumbs/system-admins-li)
      (authentication-systems.breadcrumbs/authentication-systems-li)])
   [:div
    [:h1 "System"
     ]]])
