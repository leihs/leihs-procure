(ns leihs.admin.resources.buildings.building.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :refer [reaction]]))

(defn post [& args]
  (go (when (some->
             {:url (path :building (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate! (path :buildings)))))

(def open?*
  (reaction
   (->> (:query-params @routing/state*)
        :action
        (= "delete"))))

(defn dialog []
  [:> Modal {:size "sm"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Delete Building"]]
   [:> Modal.Body
    "Are you sure you want to delete this building? This action cannot be undone."]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:variant "danger"
                :onClick #(post)}
     "Delete"]]])

(defn button []
  [:<>
   [:> Button
    {:variant "danger"
     :className "ml-3"
     :on-click #(search-params/append-to-url
                 {:action "delete"})}
    "Delete"]])
