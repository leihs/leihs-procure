(ns leihs.admin.resources.suppliers.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
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
   [leihs.admin.resources.suppliers.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.suppliers.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.auth.core :as auth :refer []]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [reagent.core :as reagent]))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defonce inventory-pools-data* (reagent/atom nil))

(defn fetch-suppliers []
  (http/route-cached-fetch data*))

(defn fetch-inventory-pools []
  (go (reset! inventory-pools-data*
              (some->
               {:chan (async/chan)
                :url (path :inventory-pools {} {:with_items_from_suppliers "yes"
                                                :active "yes"
                                                :per-page 1000})}
               http/request :chan <!
               http/filter-success!
               :body))))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))

(defn link-to-supplier
  [supplier inner & {:keys [authorizers]
                     :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :supplier {:supplier-id (:id supplier)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/form-term-filter-component]
     [routing/select-component
      :label "Inventory Pool"
      :query-params-key :inventory_pool_id
      :options (cons ["" "(any)"]
                     (->> @inventory-pools-data*
                          :inventory-pools
                          (map #(do [(:id %) (:name %)]))))]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [supplier]
  [:td {:key :name}
   [link-to-supplier supplier [:span (:name supplier)]
    :authorizers [auth/admin-scopes?]]])

(defn items-count-th-component []
  [:th.text-left {:key :count_items} "# Items"])

(defn items-count-td-component [supplier]
  [:td.text-left {:key :items_count} (:count_items supplier)])

;;;;;

(defn suppliers-thead-component [more-cols]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    (for [[idx col] (map-indexed vector more-cols)]
      ^{:key idx} [col])]])

(defn supplier-row-component [supplier more-cols]
  ^{:key (:id supplier)}
  [:tr.supplier {:key (:id supplier)}
   [:td {:key :index} (:index supplier)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col supplier])])

(defn core-table-component [hds tds suppliers]
  (if-let [suppliers (seq suppliers)]
    [:table.suppliers.table.table-striped.table-sm
     [suppliers-thead-component hds]
     [:tbody
      (let [page (:page @current-query-paramerters-normalized*)
            per-page (:per-page @current-query-paramerters-normalized*)]
        (doall (for [supplier suppliers]
                 ^{:key (:id supplier)}
                 [supplier-row-component supplier tds])))]]
    [:div.alert.alert-warning.text-center "No (more) suppliers found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :suppliers)]))

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
      [:h3 "@inventory-pools-data*"]
      [:pre (with-out-str (pprint @inventory-pools-data*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component
    {:did-change fetch-suppliers
     :did-mount fetch-inventory-pools}]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [name-th-component
     items-count-th-component]
    [name-td-component
     items-count-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.suppliers
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]]
   [:h1 [icons/suppliers] " Suppliers"]
   [main-page-content-component]])
