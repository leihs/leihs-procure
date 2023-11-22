(ns leihs.admin.resources.rooms.room.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :room-id presence)
                ":room-id")))

(defonce data* (reagent/atom nil))

(defonce buildings-data* (reagent/atom nil))

(defn clean-and-fetch []
  (reset! data* nil)
  (reset! buildings-data* nil)
  (go (reset! buildings-data*
              (some-> {:chan (async/chan)
                       :url (path :buildings)}
                      http/request :chan <!
                      http/filter-success!
                      :body :buildings))
      (if (= (:route @routing/state*) (path :rooms))
        (reset! data* {:building_id nil})
        (reset! data*
                (some->
                 {:chan (async/chan)
                  :url (path :room
                             (-> @routing/state* :route-params))}
                 http/request :chan <!
                 http/filter-success! :body)))
      (http/route-cached-fetch data*)))

(defn room-form [action]
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
     (->> @buildings-data*
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
       [:pre (with-out-str (pprint @buildings-data*))]]]]))
