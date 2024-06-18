(ns leihs.admin.resources.rooms.room.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.rooms.room.core :as core]
   [leihs.admin.resources.rooms.room.delete :as delete]
   [leihs.admin.resources.rooms.room.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn get-building-name [id]
  (->> @core/data-buildings*
       (filter #(= (:id %) id))
       (first)
       :name))

(defn info-table []
  [table/container
   {:className "room"
    :borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td.name (:name @core/data*)]]

     [:tr.description
      [:td "Description" [:small " (description)"]]
      [:td.description
       {:style {:white-space "break-spaces"}}
       (:description @core/data*)]]

     [:tr.building
      [:td "Building" [:small " (building)"]]
      [:td.building (get-building-name (:building_id @core/data*))]]]}])

(defn header []
  (let [name (:name @core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main {:to (path :rooms)}]
       [:h1.mt-3
        [:span " Room " name]]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount (fn []
                  (core/fetch-room)
                  (core/fetch-buildings))
     :will-unmount (fn []
                     (reset! core/data* nil)
                     (reset! core/data-buildings* nil))}]

   (if-not (and @core/data*
                @core/data-buildings*)
     [:div.my-5
      [wait-component]]

     [:article.room
      [header]
      [:section
       [info-table]
       [edit/button]
       [edit/dialog]
       [delete/button]
       [delete/dialog]]

      [core/debug-component]])])
