(ns leihs.admin.resources.suppliers.supplier.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as supplier-core :refer [clean-and-fetch]]
   [leihs.admin.resources.suppliers.supplier.delete :as delete]
   [leihs.admin.resources.suppliers.supplier.edit :as edit]
   [leihs.admin.resources.suppliers.supplier.items :as items]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn info-table []
  (let [data @supplier-core/data*]
    (fn []
      [table/container
       {:borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td.name (:name data)]]
         [:tr.description
          [:td "Note" [:small " (note)"]]
          [:td.note {:style {:white-space "break-spaces"}} (:note data)]]]}])))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))

(defn delete-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:variant "danger"
         :className "ml-3"
         :onClick #(reset! show true)}
        "Delete"]
       [delete/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn header []
  (let [name (:name @supplier-core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :suppliers)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (if-not @supplier-core/data*
     [:div.my-5
      [wait-component " Loading Room Data ..."]]
     [:article.supplier
      [header]
      [info-table]
      [edit-button]
      [delete-button]
      [items/component]
      [supplier-core/debug-component]])])
