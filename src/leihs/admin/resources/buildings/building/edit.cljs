(ns leihs.admin.resources.buildings.building.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.building.core :as core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(def data* (reagent/atom nil))

(defn patch []
  (let [route (path :building {:building-id @core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (swap! core/cache* assoc @core/path* @data*)
          (search-params/delete-from-url "action")))))

(def open?*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit"))))

(defn dialog []
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Edit Building"]]
   [:> Modal.Body
    [core/building-form patch data*]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "building-form"}
     "Save"]]])

(defn button []
  [:<>
   [:> Button
    {:on-click #(search-params/append-to-url
                 {:action "edit"})}
    "Edit"]])
