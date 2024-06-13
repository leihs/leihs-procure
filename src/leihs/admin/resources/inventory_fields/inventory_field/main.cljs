(ns leihs.admin.resources.inventory-fields.inventory-field.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as inventory-field
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
  (let [data @inventory-field/data*]
    (fn []
      [:<>
       [table/container
        {:borders false
         :header [:tr [:th "Property"] [:th.w-75 "Value"]]
         :body
         [:<>
          [:tr.active
           [:td "Active" [:small " (name)"]]
           [:td.active (str (:active data))]]
          [:tr.label
           [:td "Label" [:small " (data:label)"]]
           [:td.label (-> data :data :label)]]
          (when (:dynamic @inventory-field-data*)
            [:<>
             [:tr.attribute
              [:td "Unique ID-Attribute" [:small " (data:attribute)"]]
              [:td.attribute (nth (-> data :data :attribute) 1)]]
             [:tr.forPackage
              [:td "Enabled for packages" [:small " (data:forPackage)"]]
              [:td.forPackage (str (or (-> data :data :forPackage) false))]]
             [:tr.owner
              [:td "Editable by owner only" [:small " (data:permissions:owner)"]]
              [:td.owner (str (or (-> data :data :permissions :owner) false))]]
             [:tr.role
              [:td "Minimum role required for view" [:small " (data:permissions:role)"]]
              [:td.role (-> data :data :permissions :role)]]
             [:tr.field-group
              [:td "Field Group" [:small " (data:group)"]]
              [:td.field-group (or (-> data :data :group) "None")]]
             [:tr.target-type
              [:td "Target" [:small " (data:target_type)"]]
              [:td.target-type (or (-> data :data :target_type) "License+Item")]]
             [:tr.type
              [:td "Type" [:small " (data:type)"]]
              [:td.type (-> data :data :type)]]])]}]])))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed?
             [auth/admin-scopes?])
        [:<>
         [:> Button
          {:onClick #(reset! show true)}
          "Edit"]
         [edit/dialog {:show @show
                       :onHide #(reset! show false)}]]))))

(defn delete-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (and
             (:dynamic @inventory-field-data*)
             (-> @inventory-field-data* :data :required not)
             (= @inventory-field-usage-data* 0))
        [:<>
         [:> Button
          {:variant "danger"
           :className "ml-3"
           :onClick #(reset! show true)}
          "Delete"]
         [delete/dialog {:show @show
                         :onHide #(reset! show false)}]]))))

(defn header []
  (let [data  @inventory-field/inventory-field-data*]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :inventory-fields)}]
       [:h1.mt-3 (-> data :data :label)]
       [:p "( " (:id data) " )"]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change inventory-field/clean-and-fetch}]
   (if-not @inventory-field/data*
     [:div.my-5
      [wait-component " Loading Data ..."]]
     [:article.room
      [header]
      [:section
       [info-table]
       [edit-button]
       [delete-button]
       [inventory-field/debug-component]]])])
