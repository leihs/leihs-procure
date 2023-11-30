(ns leihs.admin.resources.rooms.room.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [cljs.core.async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.contrib.inflect :refer [pluralize-noun]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.rooms.breadcrumbs :as breadcrumbs-parent]
   [leihs.admin.resources.rooms.room.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.rooms.room.core :as room :refer [clean-and-fetch id* data*]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [keyword str presence detect]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn room-component []
  (if-not @room/data*
    [wait-component]
    [:div.room.mt-3
     (when (:is_general @room/data*)
       [:div.alert.alert-info "This is a general (virtual) room inside a specific building which is used for unknown locations of items. It is automatically created when a new building is created. It cannot be deleted on its own, but gets deleted when the whole building is deleted."])
     [:div
      [form-components/input-component room/data* [:name]
       :label "Name"
       :required true
       :disabled (not @room/edit-mode?*)]
      [form-components/input-component room/data* [:description]
       :element :textarea
       :label "Description"
       :disabled (not @room/edit-mode?*)]
      [form-components/select-component room/data* [:building_id]
       (->> @room/buildings-data*
            (map #(vector (:id %) (:name %)))
            (cons [nil nil]))
       nil
       :label "Building"
       :required true
       :disabled (not @room/edit-mode?*)]]]))

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [route (path :room {:room-id @room/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @room/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn edit-page []
  [:div.edit-room
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]
   (breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/edit-li]) [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Room "]
      [room/name-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [room-component]
    [form-components/save-submit-component]]
   [room/debug-component]])

;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create []
  (go (when-let [id (some->
                     {:url (path :rooms)
                      :method :post
                      :json-params  @room/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :room {:room-id id})))))

(defn create-submit-component []
  (if @room/edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       " Create "]]
     [:div.clearfix]]))

(defn create-page []
  [:div.new-room
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]
   (breadcrumbs/nav-component
    (conj @breadcrumbs-parent/left*
          [breadcrumbs-parent/create-li])
    [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Room "]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (create))}
    [room-component]
    [create-submit-component]]
   [room/debug-component]])

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-room [& args]
  (go (when (some->
             {:url (path :room (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate! (path :rooms)))))

(defn delete-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-room))}
   [form-components/delete-submit-component]])

(defn delete-page []
  [:div.group-delete
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Room "
    [room/name-link-component]]
   [room/id-component]
   [delete-form-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.room
   [routing/hidden-state-component {:did-mount clean-and-fetch}]
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/rooms-li]
     [breadcrumbs/delete-li]
     [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Room "]
      [room/name-link-component]]]]
   [room-component]
   [room/debug-component]])
