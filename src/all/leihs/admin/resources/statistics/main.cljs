(ns leihs.admin.resources.statistics.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

[leihs.admin.resources.statistics.breadcrumbs :as breadcrumbs-statistics]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.statistics.basic :as statistics.basic]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defn page []
  [:div.statistics
   [ breadcrumbs/nav-component
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [breadcrumbs-statistics/statistics-li]][]]
   [:h1 "Statistics"]
   [statistics.basic/main]
   ])
