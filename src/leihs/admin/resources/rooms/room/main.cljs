(ns leihs.admin.resources.rooms.room.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.rooms.room.core :as room :refer [clean-and-fetch]]
   [leihs.admin.resources.rooms.room.delete :as delete]
   [leihs.admin.resources.rooms.room.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-building-name [id]
  (->> @room/buildings-data*
       (filter #(= (:id %) id))
       (first)
       :name))

(defn info-table []
  (let [data @room/data*]
    (fn []
      [table/container
       {:className "room"
        :borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td.name (:name data)]]
         [:tr.description
          [:td "Description" [:small " (description)"]]
          [:td.description {:style {:white-space "break-spaces"}} (:description data)]]
         [:tr.building
          [:td "Building" [:small " (building)"]]
          [:td.building (get-building-name (:building_id data))]]]}])))

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
  (let [name (:name @room/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main {:to (path :rooms)}]
       [:h1.mt-3
        [:span " Room " name]]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (if-not @room/data*
     [:div.my-5
      [wait-component " Loading Room Data ..."]]
     [:article.room
      [header]
      [:section
       [info-table]
       [edit-button]
       [delete-button]
       [room/debug-component]]])])
