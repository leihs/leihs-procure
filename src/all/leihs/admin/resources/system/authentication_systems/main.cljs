(ns leihs.admin.resources.system.authentication-systems.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.defaults :as defaults]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]

    [leihs.admin.utils.seq :refer [with-index]]
    [leihs.admin.resources.system.authentication-systems.shared :as shared]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-route* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))


(def data* (reagent/atom {}))

(defn fetch-authentication-systems []
  (http-client/route-cached-fetch data*))




;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authentication-systems-thead-component []
  [:thead
   [:tr
    [:th "Index"]
    [:th "Id"]
    [:th "Enabled"]
    [:th "Type"]
    [:th "Priority"]
    [:th "# Users"]
    [:th "Name"]]])

(defn link-to-authentication-system [authentication-system inner]
  [:a {:href (path :authentication-system {:authentication-system-id (:id authentication-system)})}
   inner])

(defn authentication-system-row-component [authentication-system]
  [:tr.authentication-system {:key (:id authentication-system)}
   [:td (link-to-authentication-system authentication-system (:index authentication-system))]
   [:td (link-to-authentication-system authentication-system (:id authentication-system))]
   [:td (-> authentication-system :enabled str)]
   [:td (:type authentication-system)]
   [:td (:priority authentication-system)]
   [:td (:count_users authentication-system)]
   [:td (link-to-authentication-system authentication-system (:name authentication-system))]])

(defn authentication-systems-table-component []
  (if-not (contains? @data* @current-route*)
    [wait-component]
    (if-let [authentication-systems (-> @data* (get  @current-route* {}) :authentication-systems seq)]
      [:table.table.table-striped.table-sm
       [authentication-systems-thead-component]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [authentication-system authentication-systems]
                   (authentication-system-row-component authentication-system))))]]
      [:div.alert.alert-warning.text-center "No (more) authentication-systems found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-paramerters-normalized*"]
      [:pre (with-out-str (pprint @current-query-paramerters-normalized*))]]
     [:div
      [:h3 "@current-route*"]
      [:pre (with-out-str (pprint @current-route*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component
    {:did-change fetch-authentication-systems }]
   [filter-component]
   [routing/pagination-component]
   [authentication-systems-table-component]
   [routing/pagination-component]
   [debug-component]])

(defn breadcrumbs []
  (breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]))

(defn page []
  [:div.authentication-systems
   [breadcrumbs]
   [:h1 "Authentication-Systems"]
   [main-page-content-component]])
