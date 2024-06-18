(ns leihs.admin.resources.inventory-fields.inventory-field.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as core
    :refer [inventory-field-data*
            inventory-field-usage-data*]]
   [leihs.admin.resources.inventory-fields.inventory-field.delete :as delete]
   [leihs.admin.resources.inventory-fields.inventory-field.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn info-table []
  [:<>
   [table/container
    {:borders false
     :header [:tr [:th "Property"] [:th.w-75 "Value"]]
     :body
     [:<>
      [:tr.active
       [:td "Active" [:small " (name)"]]
       [:td.active (str (:active @core/data*))]]
      [:tr.label
       [:td "Label" [:small " (data:label)"]]
       [:td.label (-> @core/data* :data :label)]]
      (when (:dynamic @core/inventory-field-data*)
        [:<>
         [:tr.attribute
          [:td "Unique ID-Attribute" [:small " (data:attribute)"]]
          [:td.attribute (nth (-> @core/data* :data :attribute) 1)]]
         [:tr.forPackage
          [:td "Enabled for packages" [:small " (data:forPackage)"]]
          [:td.forPackage (str (or (-> @core/data* :data :forPackage) false))]]
         [:tr.owner
          [:td "Editable by owner only" [:small " (data:permissions:owner)"]]
          [:td.owner (str (or (-> @core/data* :data :permissions :owner) false))]]
         [:tr.role
          [:td "Minimum role required for view" [:small " (data:permissions:role)"]]
          [:td.role (-> @core/data* :data :permissions :role)]]
         [:tr.field-group
          [:td "Field Group" [:small " (data:group)"]]
          [:td.field-group (or (-> @core/data* :data :group) "None")]]
         [:tr.target-type
          [:td "Target" [:small " (data:target_type)"]]
          [:td.target-type (or (-> @core/data* :data :target_type) "License+Item")]]
         [:tr.type
          [:td "Type" [:small " (data:type)"]]
          [:td.type (-> @core/data* :data :type)]]])]}]])

(defn header []
  (let [data  @core/inventory-field-data*]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :inventory-fields)}]
       [:h1.mt-3 (-> data :data :label)]
       [:p "( " (:id data) " )"]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount core/clean-and-fetch}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]

     [:article.room
      [header]
      [:section
       [info-table]
       [edit/button]
       [edit/dialog]
       [delete/button]
       [delete/dialog]
       [core/debug-component]]])])
