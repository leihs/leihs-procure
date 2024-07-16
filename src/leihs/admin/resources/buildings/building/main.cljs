(ns leihs.admin.resources.buildings.building.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.building.core :as core]
   [leihs.admin.resources.buildings.building.delete :as delete]
   [leihs.admin.resources.buildings.building.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn building-info-table []
  [table/container
   {:className "building"
    :borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td.name  (:name @core/data*)]]
     [:tr.code
      [:td "Code" [:small " (code)"]]
      [:td.code (:code @core/data*)]]]}])

(defn header []
  (let [name (:name @core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main {:to (path :buildings)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]

     [:article.building
      [header]

      [:section
       [building-info-table]
       [edit/button]
       [edit/dialog]
       [delete/button]
       [delete/dialog]]

      [core/debug-component]])])
