(ns leihs.admin.resources.suppliers.supplier.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as core]
   [leihs.admin.resources.suppliers.supplier.delete :as delete]
   [leihs.admin.resources.suppliers.supplier.edit :as edit]
   [leihs.admin.resources.suppliers.supplier.items :as items]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn info-table []
  [table/container
   {:borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td.name (:name @core/data*)]]
     [:tr.description
      [:td "Note" [:small " (note)"]]
      [:td.note {:style {:white-space "break-spaces"}} (:note @core/data*)]]]}])

(defn header []
  (let [name (:name @core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :suppliers)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount (fn []
                  (core/fetch)
                  (core/fetch-items))
     :will-unmount (fn []
                     (reset! core/data* nil)
                     (reset! core/data-items* nil))}]

   (if-not (or
            @core/data*
            @core/data-items*)
     [:div.my-5
      [wait-component]]

     [:article.supplier
      [header]

      [:section
       [info-table]
       [edit/button]
       [edit/dialog]
       [delete/button]
       [delete/dialog]]

      [:section
       [items/component]]

      [core/debug-component]])])
