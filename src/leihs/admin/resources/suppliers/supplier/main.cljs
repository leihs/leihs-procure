(ns leihs.admin.resources.suppliers.supplier.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.suppliers.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.resources.suppliers.supplier.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.suppliers.supplier.core :as supplier :refer [clean-and-fetch id* data*] ]
    [leihs.admin.resources.suppliers.supplier.items :as items]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defonce edit-mode?*
  (reaction
    (and (map? @data*)
         (boolean ((set '(:supplier-edit :supplier-create))
                   (:handler-key @routing/state*))))))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn supplier-component []
  (if-not @supplier/data*
    [wait-component]
    [:div.supplier.mt-3
     [:div
      [form-components/input-component supplier/data* [:name]
       :label "Name"
       :required true
       :disabled (not @supplier/edit-mode?*)]]
     [form-components/input-component supplier/data* [:note]
      :label "Note"
      :element :textarea
      :rows 20
      :disabled (not @supplier/edit-mode?*)]]))

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [route (path :supplier {:supplier-id @supplier/id*})]
  (go (when (some->
              {:url route
               :method :patch
               :json-params  @supplier/data*
               :chan (async/chan)}
              http-client/request :chan <!
              http-client/filter-success!)
        (accountant/navigate! route)))))

(defn edit-page []
  [:div.edit-supplier
   [routing/hidden-state-component
    {:did-mount supplier/clean-and-fetch}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs/left*
           [breadcrumbs/edit-li])[])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Supplier "]
      [supplier/name-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [supplier-component]
    [form-components/save-submit-component]]
   [supplier/debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create []
  (go (when-let [id (some->
                      {:url (path :suppliers)
                       :method :post
                       :json-params  @supplier/data*
                       :chan (async/chan)}
                      http-client/request :chan <!
                      http-client/filter-success!
                      :body :id)]
        (accountant/navigate!
          (path :supplier {:supplier-id id})))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       " Create "]]
     [:div.clearfix]]))

(defn create-page []
  [:div.new-supplier
   [routing/hidden-state-component
    {:did-mount #(reset! supplier/data* {})}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs-parent/left*
           [breadcrumbs-parent/create-li])
     [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Supplier "]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (create))}
    [supplier-component]
    [create-submit-component]]
   [supplier/debug-component]])

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-supplier [& args]
  (go (when (some->
              {:url (path :supplier (-> @routing/state* :route-params))
               :method :delete
               :chan (async/chan)}
              http-client/request :chan <!
              http-client/filter-success!)
        (accountant/navigate! (path :suppliers)))))

(defn delete-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-supplier))}
   [form-components/delete-submit-component]])

(defn delete-page []
  [:div.group-delete
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Supplier "
    [supplier/name-link-component]]
   [supplier/id-component]
   [delete-form-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.supplier
   [routing/hidden-state-component {:did-mount #(clean-and-fetch)}]
   [breadcrumbs/nav-component
     @breadcrumbs/left*
     [[breadcrumbs/suppliers-li]
      [breadcrumbs/delete-li]
      [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Supplier "]
      [supplier/name-link-component]]]]
   [supplier-component]
   [items/component]
   [supplier/debug-component]])
