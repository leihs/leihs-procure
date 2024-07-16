(ns leihs.admin.resources.suppliers.supplier.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id*
  (reaction
   (or (-> @routing/state* :route-params :supplier-id presence)
       ":supplier-id")))

(defonce path*
  (reaction
   (path :supplier {:supplier-id @id*})))

(defonce cache* (reagent/atom nil))
(defonce data*
  (reaction
   (get @cache* @path*)))

(defn fetch []
  (http-client/route-cached-fetch
   cache* {:route @path*}))

(defonce path-items*
  (reaction
   (path :supplier-items {:supplier-id @id*})))

(defonce cache-items* (reagent/atom nil))
(defonce data-items*
  (reaction
   (:items (get @cache-items* @path-items*))))

(defn fetch-items []
  (http-client/route-cached-fetch
   cache-items* {:route @path-items*}))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.supplier-debug
     [:hr]
     [:div.supplier-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn form [action data*]
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
