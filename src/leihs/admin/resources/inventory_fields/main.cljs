(ns leihs.admin.resources.inventory-fields.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.create :as create]
   [leihs.admin.resources.inventory-fields.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert Button]]
   [reagent.core :as reagent]))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defonce inventory-fields-groups-data* (reagent/atom nil))

(defn fetch-inventory-fields []
  (http/route-cached-fetch data*))

(defn fetch-inventory-fields-groups []
  (go (reset! inventory-fields-groups-data*
              (some-> {:chan (async/chan)
                       :url (path :inventory-fields-groups)}
                      http/request :chan <!
                      http/filter-success!
                      :body :inventory-fields-groups sort))))

(defn link-to-inventory-field
  [inventory-field inner & {:keys [authorizers] :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :inventory-field {:inventory-field-id (:id inventory-field)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [filter/container
   [:<>
    [filter/form-term-filter-component {:placeholder "Enter Inventory Field Name"}]
    [filter/select-component
     :label "Target-Type"
     :query-params-key :target_type
     :options {nil "(both)"
               "item" "Item"
               "license" "License"}]
    [filter/select-component
     :label "Configurable"
     :query-params-key :dynamic
     :options {nil "(any value)"
               "yes" "yes"
               "no" "no"}]
    [filter/select-component
     :label "Active"
     :query-params-key :active
     :options {nil "(any value)"
               "yes" "yes"
               "no" "no"}]
    [filter/select-component
     :label "Form-Group"
     :query-params-key :group
     :options (concat [[nil "(any value)"] ["none" "<none>"]]
                      @inventory-fields-groups-data*)]
    [filter/form-per-page]
    [filter/reset]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-inventory-field-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:className "ml-3"
         :onClick #(reset! show true)}
        "Add Field"]
       [create/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn id-th-component []
  [:th {:key :id} "ID"])

(defn id-td-component [inventory-field]
  [:td {:key :id}
   [link-to-inventory-field inventory-field [:span (:id inventory-field)]
    :authorizers [auth/admin-scopes?]]])

(defn target-type-th-component []
  [:th {:key :target-type} "Target-Type"])

(defn target-type-td-component [inventory-field]
  [:td.text-left {:key :target-type}
   (case (-> inventory-field :data :target_type)
     "item" "Item"
     "license" "License"
     nil "Item+License"
     "n/a")])

(defn dynamic-th-component []
  [:th {:key :dynamic} "Configurable"])

(defn dynamic-td-component [inventory-field]
  [:td.text-left {:key :dynamic} (case (:dynamic inventory-field)
                                   true "yes"
                                   false "no"
                                   "n/a")])

(defn active-th-component []
  [:th {:key :active} "Active"])

(defn active-td-component [inventory-field]
  [:td.text-left {:key :active} (case (:active inventory-field)
                                  true "yes"
                                  false "no"
                                  "n/a")])

(defn label-th-component []
  [:th {:key :label} "Label"])

(defn label-td-component [inventory-field]
  [:td.text-left {:key :label} (-> inventory-field :data :label)])

(defn group-th-component []
  [:th {:key :form-group} "Form-Group"])

(defn group-td-component [inventory-field]
  [:td.text-left {:key :form-group} (-> inventory-field :data :group)])

;;;;;

(defn inventory-fields-thead-component [more-cols]
  [:tr
   [:th {:key :index} "Index"]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col])])

(defn inventory-field-row-component [inventory-field more-cols]
  ^{:key (:id inventory-field)}
  [:tr.inventory-field {:key (:id inventory-field)}
   [:td {:key :index} (:index inventory-field)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col inventory-field])])

(defn core-table-component [hds tds inventory-fields]
  (if-let [inventory-fields (seq inventory-fields)]
    [table/container
     {:className "inventory-fields"
      :actions [table/toolbar [add-inventory-field-button]]
      :header  [inventory-fields-thead-component hds]
      :body (doall (for [inventory-field inventory-fields]
                     ^{:key (:id inventory-field)}
                     [inventory-field-row-component inventory-field tds]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No (more) inventory-fields found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :inventory-fields)]))

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
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@inventory-fields-groups-data*"]
      [:pre (with-out-str (pprint @inventory-fields-groups-data*))]]]))

(defn page []
  [:article.inventory-fields
   [:header.my-5
    [:h1 [icons/table-list] " Inventory-Fields"]]
   [:section
    [routing/hidden-state-component
     {:did-change fetch-inventory-fields
      :did-mount fetch-inventory-fields-groups}]
    [filter-component]
    [table-component
     [id-th-component
      target-type-th-component
      dynamic-th-component
      active-th-component
      label-th-component
      group-th-component]
     [id-td-component
      target-type-td-component
      dynamic-td-component
      active-td-component
      label-td-component
      group-td-component]]
    [debug-component]]])
