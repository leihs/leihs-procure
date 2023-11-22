(ns leihs.admin.resources.system.authentication-systems.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.create :as create]
   [leihs.admin.resources.system.authentication-systems.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

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
  [filter/container
   [:<>
    [filter/form-per-page]
    [filter/reset]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:className "ml-3"
         :onClick #(reset! show true)}
        "Add Authentication System"]
       [create/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn authentication-systems-thead-component []
  [:tr
   [:th "Index"]
   [:th "Id"]
   [:th "Enabled"]
   [:th "Type"]
   [:th "Priority"]
   [:th "# Users"]
   [:th "Name"]])

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
      [table/container  {:borders true
                         :actions [table/toolbar [add-button]]
                         :header [authentication-systems-thead-component]
                         :body (doall (for [authentication-system authentication-systems]
                                        (authentication-system-row-component authentication-system)))}]
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
    {:did-change fetch-authentication-systems}]
   [filter-component]
   [table/pagination]
   [authentication-systems-table-component]
   [table/pagination]
   [debug-component]])

(defn page []
  [:article.authentication-systems
   [:header.my-5
    [:h1 [icons/key-icon] " Authentication Systems"]]
   [:section
    [routing/hidden-state-component
     {:did-change fetch-authentication-systems}]
    [filter-component]
    [authentication-systems-table-component]
    [debug-component]]])
