(ns leihs.admin.resources.status.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.status.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.core.icons :as icons]

    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce status-info-data* (reagent/atom nil))

(defn fetch-status-info []
  (go (reset! status-info-data*
              (some->
                {:url (path :status)
                 :chan (async/chan)}
                http-client/request :chan <!
                http-client/filter-success!
                :body))))

(defn info-page []
  [:div.status
   [routing/hidden-state-component
    {:did-mount fetch-status-info}]
   [breadcrumbs/nav-component @breadcrumbs/left* []]
   [:h1 "Leihs-Admin Server-Status Info"]
   [:p "The data shown below is mostly of interest for monitoring or debugging."]
   (when-let [status-info-data @status-info-data*]
     [:pre.bg-light
      (with-out-str (pprint status-info-data))])])

