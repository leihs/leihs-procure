(ns leihs.admin.resources.audits.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.audits.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.audits.changes.breadcrumbs :as changes-breadcrumbs]
    [leihs.admin.resources.audits.requests.breadcrumbs :as requests-breadcrumbs]
    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


(defn page []
  [:div.audits-page
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[changes-breadcrumbs/changes-li]
     [requests-breadcrumbs/requests-li]]]
   [:h1 audits/icon-audits "Audits"]])
