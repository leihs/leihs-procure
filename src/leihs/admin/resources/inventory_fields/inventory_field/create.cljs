(ns leihs.admin.resources.inventory-fields.inventory-field.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :inventory-fields)
                      :method :post
                      :json-params (core/strip-of-uuids
                                    @core/data*)
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :inventory-field {:inventory-field-id id})))))

(defn form []
  (if-not @core/data*
    [wait-component]
    [:div.inventory-field.mt-3
     [:div.row
      [:div.col-md
       [:> Form {:id "inventory-field-form"
                 :className "inventory-field mt-3"
                 :on-submit (fn [e] (.preventDefault e) (create))}
        [core/dynamic-inventory-field-form-component core/data*]]]
      [:div.col-md
       [core/inventory-field-data-component core/data*]]]]))

(def open?*
  (reaction
   (core/clean-and-fetch-for-new)
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog []
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Add a Field"]]
   [:> Modal.Body
    [form]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "inventory-field-form"}
     "Save"]]])

(defn button []
  [:<>
   [:> Button
    {:className "ml-3"
     :on-click #(search-params/append-to-url
                 {:action "add"})}
    "Add Field"]])
