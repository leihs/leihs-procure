(ns leihs.admin.resources.buildings.building.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.building.core :as building :refer [clean-and-fetch]]
   [leihs.admin.resources.buildings.building.delete :as delete]
   [leihs.admin.resources.buildings.building.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn building-info-table []
  (let [data @building/data*]
    (fn []
      [table/container
       {:className "building"
        :borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td.name  (:name data)]]
         [:tr.code
          [:td "Code" [:small " (code)"]]
          [:td.code (:code data)]]]}])))

(defn edit-building-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))

(defn delete-building-button []
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
  (let [name (:name @building/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main {:to (path :buildings)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (if-not @building/data*
     [:div.my-5
      [wait-component " Loading Room Data ..."]]
     [:article.building
      [header]
      [building-info-table]
      [edit-building-button]
      [delete-building-button]
      [building/debug-component]])])
