(ns leihs.admin.resources.buildings.building.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :building-id presence)
                ":building-id")))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :building
                           (-> @routing/state* :route-params))}

               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))

(defn building-form [action]
  [:div.building.mt-3
   (when (:is_general @data*)
     [:div.alert.alert-info "This is a general building which is used for unknown locations of items."])
   [:> Form {:id "building-form"
             :on-submit (fn [e] (.preventDefault e) (action))}
    [:> Form.Group {:control-id "name"}
     [:> Form.Label "Name"]
     [:input.form-control
      {:id "name"
       :type "text"
       :required true
       :value (or (:name @data*) "")
       :onChange (fn [e] (swap! data* assoc :name (-> e .-target .-value)))}]]
    [:> Form.Group {:control-id "code"}
     [:> Form.Label "Code"]
     [:input.form-control
      {:id "code"
       :type "text"
       :value (or (:code @data*) "")
       :onChange (fn [e] (swap! data* assoc :code (-> e .-target .-value)))}]]]])

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.building-debug
     [:hr]
     [:div.building-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))
