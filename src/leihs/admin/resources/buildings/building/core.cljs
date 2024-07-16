(ns leihs.admin.resources.buildings.building.core
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
  (reaction (or (-> @routing/state* :route-params :building-id presence)
                ":building-id")))

(defonce cache* (reagent/atom nil))

(defonce path*
  (reaction
   (path :building {:building-id @id*})))

@(defonce data*
   (reaction (get @cache* @path*)))

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(defn building-form [action data*]
  [:div.building.mt-3
   (when (:is_general @data*)
     [:div.alert.alert-info "This is a general building which is used for unknown locations of items."])

   [:> Form {:id "building-form"
             :on-submit (fn [e]
                          (.preventDefault e)
                          (action))}
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
