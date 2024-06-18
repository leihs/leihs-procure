(ns leihs.admin.resources.inventory-fields.inventory-field.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(def data* (reagent/atom nil))

(defn patch []
  (let [route (path :inventory-field
                    {:inventory-field-id @core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params (core/strip-of-uuids @data*)
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (reset! core/data* @data*)
          (search-params/delete-from-url "action")))))

(defn form []
  (if-not @core/data*
    [wait-component]
    [:div.inventory-field.mt-3
     (if-not (:dynamic @core/inventory-field-data*)
       [:> Alert {:variant "info"
                  :className "text-center"}
        "Some of the attributes of this inventory field cannot be edited "
        "because it belongs the the core group of inventory fields of the system."]
       [:ul.list-unstyled
        [:li
         [:dl.row.mb-0
          [:dt.col-6 {:style {:max-width "80px"}}
           [:span "Usage:"]]
          [:dd.col-6 @core/inventory-field-usage-data*
           " Items/Licenses"]]]])
     [:div.row
      [:div.col-md

       [:> Form {:id "inventory-field-form"
                 :className "inventory-field mt-3"
                 :on-submit (fn [e] (.preventDefault e) (patch))}
        (if (:dynamic @core/inventory-field-data*)
          [core/dynamic-inventory-field-form-component data* {:isEditing? true}]
          [core/inventory-field-form-component data*])]]
      [:div.col-md
       [core/inventory-field-data-component data*]]]]))

(def open?*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Edit Field"]]
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
   (when (auth/allowed?
          [auth/admin-scopes?])
     [:> Button
      {:on-click #(search-params/append-to-url
                   {:action "edit"})}
      "Edit"])])
