(ns leihs.admin.resources.inventory-fields.inventory-field.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defn post []
  (go (when (some->
             {:url (path :inventory-field
                         (:route-params @routing/state*))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate! (path :inventory-fields)))))

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
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Delete Inventory Field"]]
   [:> Modal.Body
    [:p "Are you sure you want to delete this Inventory Field?"]
    [:p.font-weight-bold "This action cannot be undone."]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:variant "danger"
                :on-click #(post)}
     "Delete"]]])

(defn button []
  (when (and
         (:dynamic @core/inventory-field-data*)
         (-> @core/inventory-field-data* :data :required not)
         (= @core/inventory-field-usage-data* 0))
    [:<>
     [:> Button
      {:variant "danger"
       :className "ml-3"
       :on-click #(search-params/append-to-url
                   {:action "delete"})}
      "Delete"]]))
