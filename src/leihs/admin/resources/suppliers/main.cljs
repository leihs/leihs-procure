(ns leihs.admin.resources.suppliers.main
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.shared :as shared]
   [leihs.admin.resources.suppliers.supplier.core :as supplier-core]
   [leihs.admin.resources.suppliers.supplier.create :as create]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [fetch-route* wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert Button]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-parameters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-parameters*)))

(def data* (reagent/atom {}))

(defonce inventory-pools-data* (reagent/atom nil))

(defn fetch []
  (http/route-cached-fetch data* {:route @fetch-route*
                                  :reload true}))

(defn fetch-inventory-pools []
  (go (reset! inventory-pools-data*
              (some->
               {:chan (async/chan)
                :url (path :inventory-pools {}
                           {:with_items_from_suppliers "yes"
                            :active "yes"
                            :per-page 1000})}
               http/request :chan <!
               http/filter-success!
               :body))))

(defn link-to-supplier
  [supplier inner & {:keys [authorizers]
                     :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :supplier {:supplier-id (:id supplier)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [filter/container
   [:<>
    [filter/form-term-filter-component {:placeholder "Supplier Name"}]
    [filter/select-component
     :label "Inventory Pool"
     :query-params-key
     :inventory_pool_id
     :options (cons ["" "(any)"]
                    (->> @inventory-pools-data*
                         :inventory-pools
                         (map #(do [(:id %) (:name %)]))))]
    [filter/form-per-page]
    [filter/reset]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-supplier-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:className "ml-3"
         :onClick #(reset! show true)}
        "Add Supplier"]
       [create/dialog {:show @show
                       :onHide #(reset! show false)}]])))

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

(defn suppliers-thead-component [more-cols]
  [:tr
   [:th {:key :index} "Index"]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col])])

(defn supplier-row-component [supplier more-cols]
  ^{:key (:id supplier)}
  [:tr.supplier {:key (:id supplier)}
   [:td {:key :index} (:index supplier)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col supplier])])

(defn core-table-component [hds tds suppliers]
  (if-let [suppliers (seq suppliers)]
    [table/container
     {:className "suppliers"
      :header [suppliers-thead-component hds]
      :body (doall (for [supplier suppliers]
                     ^{:key (:id supplier)}
                     [supplier-row-component supplier tds]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No (more) suppliers found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* @fetch-route*)
    [wait-component]
    [core-table-component hds tds (-> @data*
                                      (get @fetch-route*)
                                      :suppliers)]))

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
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@inventory-pools-data*"]
      [:pre (with-out-str (pprint @inventory-pools-data*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change #(fetch)
     :did-mount #(fetch-inventory-pools)}]

   [:article.suppliers
    [:header.my-5
     [:h1 [icons/suppliers] " Suppliers"]]

    [:section
     [filter-component]
     [table/toolbar [create/button]]
     [table-component
      [name-th-component
       items-count-th-component]
      [name-td-component
       items-count-td-component]]

     [table/toolbar [create/button]]
     [create/dialog]]

    [debug-component]]])
