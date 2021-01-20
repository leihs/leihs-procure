(ns leihs.admin.resources.inventory-pools.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.json :as json]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.auth.core :as auth-core]


    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.defaults :as defaults]
    [leihs.admin.common.components :as components]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.resources.inventory-pools.breadcrumbs :as breadcrumbs]

    [leihs.admin.utils.seq :as seq :refer [with-index]]
    [leihs.admin.resources.inventory-pools.shared :as shared]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)
                       :order (some-> @routing/state* :query-params
                                      :order clj->js json/to-json)))))

(def current-url* (reaction (:url @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))


(def data* (reagent/atom {}))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [routing/form-term-filter-component]
    [routing/select-component
     :label "Active"
     :query-params-key :active
     :options {"" "any value" "yes" "yes" "no" "no"}]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pools-thead-component [& [more-cols]]
  [:thead
   [:tr
    [:th "Index"]

    [:th "Active"]
    [:th "Short name"]
    [:th "Name " [:a {:href (page-path-for-query-params
                              {:order (-> [[:name :asc] [:id :asc]]
                                          clj->js json/to-json)})} "↓"]]
    [:th "# Users " [:a {:href (page-path-for-query-params
                                 {:order (-> [[:users_count :desc] [:id :asc]]
                                             clj->js json/to-json)})} "↓"]]
    [:th "# Delegations " [:a {:href (page-path-for-query-params
                                       {:order (-> [[:delegations_count :desc] [:id :asc]]
                                                   clj->js json/to-json)})} "↓"]]
    (for [col more-cols]
      col)]])

(defn link-to-inventory-pool [inventory-pool inner]
  (let [id (:id inventory-pool)]
    (if (or
          (auth-core/current-user-admin-scopes?)
          (pool-auth/current-user-is-some-manager-of-pool? id))
      [:a {:href (path :inventory-pool {:inventory-pool-id id})}
       inner]
      [:span.text-info inner])))

(defn inventory-pool-row-component [inventory-pool more-cols]
  [:tr.inventory-pool {:key (:id inventory-pool)}
   [:td (:index inventory-pool)]
   [:td (if (:is_active inventory-pool) "yes" "no")]
   [:td (:shortname inventory-pool)]
   [:td (link-to-inventory-pool inventory-pool [:span (:name inventory-pool)])]
   [:td.users_count (:users_count inventory-pool)]
   [:td.delegations_count (:delegations_count inventory-pool)]
   (for [col more-cols]
     (col inventory-pool))])

(defn tbody-component [inventory-pools tds]
  [:tbody
   (let [page (:page @current-query-paramerters-normalized*)
         per-page (:per-page @current-query-paramerters-normalized*)]
     (doall (for [inventory-pool inventory-pools]
              (inventory-pool-row-component inventory-pool tds))))])

(defn inventory-pools-table-component [& [hds tds]]
  (if-not (contains? @data* @current-url*)
    [wait-component]
    (if-let [inventory-pools (-> @data* (get  @current-url* {}) :inventory-pools seq)]
      [:table.table.table-striped.table-sm.inventory-pools
       [inventory-pools-thead-component hds]
       [tbody-component inventory-pools tds]]
      [:div.alert.alert-warning.text-center "No (more) inventory-pools found."])))


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
   [routing/hidden-state-component
    {:did-change #(http/url-cached-fetch data*)}]
   [filter-component]
   [routing/pagination-component]
   [inventory-pools-table-component]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.inventory-pools
   (breadcrumbs/nav-component
     @breadcrumbs/left*
     [[breadcrumbs/create-li]])
   [:h1 icons/inventory-pools " Inventory-Pools"]
   [main-page-content-component]])
