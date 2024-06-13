(ns leihs.admin.resources.inventory-fields.inventory-field.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as inventory-field-core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :inventory-fields)
                      :method :post
                      :json-params (inventory-field-core/strip-of-uuids
                                    @inventory-field-core/data*)
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :inventory-field {:inventory-field-id id})))))

(defn form []
  (if-not @inventory-field-core/data*
    [wait-component]
    [:div.inventory-field.mt-3
     [:div.row
      [:div.col-md
       [:> Form {:id "inventory-field-form"
                 :className "inventory-field mt-3"
                 :on-submit (fn [e] (.preventDefault e) (create))}
        [inventory-field-core/dynamic-inventory-field-form-component]]]
      [:div.col-md
       [inventory-field-core/inventory-field-data-component]]]]))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (inventory-field-core/clean-and-fetch-for-new)
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a Field"]]
   [:> Modal.Body
    [form]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "inventory-field-form"}
     "Save"]]])
