(ns leihs.admin.resources.buildings.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.buildings.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.buildings.shared :as shared]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.auth.core :as auth :refer []]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defn fetch-buildings []
  (http/route-cached-fetch data*))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))

(defn link-to-building
  [building inner & {:keys [authorizers]
                     :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :building {:building-id (:id building)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/form-term-filter-component]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [building]
  (let [building-name (cond-> (:name building)
                        (:is_general building) (str " (general)"))
        inner-comp [:td {:key :code}
                    [link-to-building building [:span building-name]
                     :authorizers [auth/admin-scopes?]]]]
    (cond->> inner-comp
      (:is_general building) (vector :i))))

(defn code-th-component []
  [:th {:key :code} "Code"])

(defn code-td-component [building]
  [:td.text-left {:key :code} (:code building)])

(defn items-count-th-component []
  [:th.text-left {:key :items_count} "# Items"])

(defn items-count-td-component [building]
  [:td.text-left {:key :items_count} (:items_count building)])

(defn rooms-count-th-component []
  [:th.text-left {:key :rooms_count} "# Rooms"])

(defn rooms-count-td-component [building]
  [:td.text-left {:key :rooms_count} (:rooms_count building)])

;;;;;

(defn buildings-thead-component [more-cols]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    (for [[idx col] (map-indexed vector more-cols)]
      ^{:key idx} [col])]])

(defn building-row-component [building more-cols]
  ^{:key (:id building)}
  [:tr.building {:key (:id building)}
   [:td {:key :index} (:index building)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col building])])

(defn core-table-component [hds tds buildings]
  (if-let [buildings (seq buildings)]
    [:table.buildings.table.table-striped.table-sm
     [buildings-thead-component hds]
     [:tbody
      (let [page (:page @current-query-paramerters-normalized*)
            per-page (:per-page @current-query-paramerters-normalized*)]
        (doall (for [building buildings]
                 ^{:key (:id building)}
                 [building-row-component building tds])))]]
    [:div.alert.alert-warning.text-center "No (more) buildings found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :buildings)]))

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
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component {:did-change fetch-buildings}]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [name-th-component
     code-th-component
     rooms-count-th-component
     items-count-th-component]
    [name-td-component
     code-td-component
     rooms-count-td-component
     items-count-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.buildings
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]]
   [:h1 [icons/building] " Buildings"]
   [main-page-content-component]])
