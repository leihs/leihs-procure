(ns leihs.admin.resources.suppliers.supplier.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :supplier-id presence)
                ":supplier-id")))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :supplier
                           (-> @routing/state* :route-params))}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.supplier-debug
     [:hr]
     [:div.supplier-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn form [action]
  [:> Form {:id "supplier-form"
            :on-submit (fn [e] (.preventDefault e) (action))}
   [:> Form.Group {:id "name"}
    [:> Form.Label "Name"]
    [:input.form-control
     {:type "text"
      :id "name"
      :required true
      :placeholder "Enter Name"
      :value (or (:name @data*) "")
      :onChange (fn [e] (swap! data* assoc :name (-> e .-target .-value)))}]]
   [:> Form.Group {:id "note"}
    [:> Form.Label "Note"]
    [:textarea.form-control
     {:placeholder "Enter Note"
      :id "note"
      :rows 10
      :value (or (:note @data*) "")
      :onChange (fn [e] (swap! data* assoc :note (-> e .-target .-value)))}]]])
