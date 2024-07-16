(ns leihs.admin.resources.system.authentication-systems.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as syssec-core]
   [leihs.admin.resources.system.authentication-systems.authentication-system.create :as create]
   [leihs.admin.resources.system.authentication-systems.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [fetch-route* wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-route* (reaction (:route @routing/state*)))

(def current-query-parameters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-parameters*)))

(def data* (reagent/atom {}))

(defn fetch []
  (http-client/route-cached-fetch data* {:route @fetch-route*
                                         :reload true}))

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
  (if-not (contains? @data* @fetch-route*)
    [wait-component]
    (if-let [authentication-systems (-> @data*
                                        (get  @fetch-route*)
                                        :authentication-systems seq)]
      [table/container  {:borders true
                         :actions [table/toolbar [create/button]]
                         :header [authentication-systems-thead-component]
                         :body (doall (for [authentication-system authentication-systems]
                                        (authentication-system-row-component authentication-system)))}]
      [:div.alert.alert-info.text-center "No (more) authentication-systems found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-parameters-normalized*"]
      [:pre (with-out-str (pprint @current-query-parameters-normalized*))]]
     [:div
      [:h3 "@current-route*"]
      [:pre (with-out-str (pprint @current-route*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(syssec-core/reset)
     :did-change #(fetch)}]

   [:article.authentication-systems
    [:header.my-5
     [:h1 [icons/key-icon] " Authentication Systems"]]

    [:section.mb-5
     [filter-component]
     [authentication-systems-table-component]
     [create/dialog]]

    [debug-component]]])
