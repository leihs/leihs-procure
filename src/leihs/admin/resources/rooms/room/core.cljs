(ns leihs.admin.resources.rooms.room.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :room-id presence)
                ":room-id")))

(defonce data* (reagent/atom nil))
(defonce data-buildings* (reagent/atom nil))

(defn fetch-room []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :room
                           (-> @routing/state* :route-params))}
               http/request :chan <!
               http/filter-success! :body))))

(defn fetch-buildings []
  (go (reset! data-buildings*
              (some-> {:chan (async/chan)
                       :url (path :buildings)}
                      http/request :chan <!
                      http/filter-success!
                      :body :buildings))))

(defn room-form [action data*]
  [:> Form {:id "room-form"
            :on-submit (fn [e] (.preventDefault e) (action))}
   [:> Form.Group {:control-id "name"}
    [:> Form.Label "Name"]
    [:input.form-control
     {:id "name"
      :type "text"
      :required true
      :placeholder "Enter Name"
      :value (or (:name @data*) "")
      :onChange (fn [e] (swap! data* assoc :name (-> e .-target .-value)))}]]
   [:> Form.Group {:control-id "description"}
    [:> Form.Label "Description"]
    [:textarea.form-control
     {:id "description"
      :placeholder "Enter description"
      :rows 10
      :value (or (:description @data*) "")
      :onChange (fn [e] (swap! data* assoc :description (-> e .-target .-value)))}]]
   [:> Form.Group {:control-id "building_id"}
    [:> Form.Label "Building"]
    [:> Form.Control
     {:required true
      :value (or (:building_id @data*)
                 (str ""))
      :onChange (fn [e] (swap! data* assoc :building_id (-> e .-target .-value)))
      :as "select"}
     [:option {:value "" :disabled true}
      "Select Building"]
     (->> @data-buildings*
          (map-indexed
           #(vector :option {:key %1
                             :value (:id %2)}
                    (:name %2))))]]])

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div.room-debug
      [:hr]
      [:div.room-data
       [:h2 "@data*"]
       [:pre (with-out-str (pprint @data*))]]]
     [:div.room-debug
      [:hr]
      [:div.room-data
       [:h2 "@buildings-data*"]
       [:pre (with-out-str (pprint @data-buildings*))]]]]))
