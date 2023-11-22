(ns leihs.admin.resources.inventory-fields.inventory-field.edit
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as inventory-field-core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [react-bootstrap :as react-bootstrap :refer [Alert Button Form Modal]]))

(defn patch []
  (let [route (path :inventory-field
                    {:inventory-field-id @inventory-field-core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params (inventory-field-core/strip-of-uuids @inventory-field-core/data*)
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn form []
  (if-not @inventory-field-core/data*
    [wait-component]
    [:div.inventory-field.mt-3
     (if-not (:dynamic @inventory-field-core/inventory-field-data*)
       [:> Alert {:variant "info"
                  :className "text-center"}
        "Some of the attributes of this inventory field cannot be edited "
        "because it belongs the the core group of inventory fields of the system."]
       [:ul.list-unstyled
        [:li
         [:dl.row.mb-0
          [:dt.col-6 {:style {:max-width "80px"}}
           [:span "Usage:"]]
          [:dd.col-6 @inventory-field-core/inventory-field-usage-data*
           " Items/Licenses"]]]])
     [:div.row
      [:div.col-md

       [:> Form {:id "inventory-field-form"
                 :className "inventory-field mt-3"
                 :on-submit (fn [e] (.preventDefault e) (patch))}
        (if (:dynamic @inventory-field-core/inventory-field-data*)
          [inventory-field-core/dynamic-inventory-field-form-component {:isEditing? true}]
          [inventory-field-core/core-inventory-field-form-component])]]
      [:div.col-md
       [inventory-field-core/inventory-field-data-component]]]]))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Room"]]
   [:> Modal.Body
    [form]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "inventory-field-form"}
     "Save"]]])

