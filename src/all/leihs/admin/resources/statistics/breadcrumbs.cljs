(ns leihs.admin.resources.statistics.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.auth.core :as auth]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async :refer [timeout]]
    [clojure.string :refer [split trim]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ))

(defn statistics-li []
  [breadcrumbs/li :statistics
   [:span [:i.fas.fa-chart-line] " Statistics "] {} {}
   :authorizers [auth/admin-scopes?]])
