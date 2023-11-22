(ns leihs.admin.resources.suppliers.supplier.create
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as supplier-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :suppliers)
                      :method :post
                      :json-params  @supplier-core/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :supplier {:supplier-id id})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  (reset! supplier-core/data* {})
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a Supplier"]]
   [:> Modal.Body
    [supplier-core/form create]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "supplier-form"}
     "Save"]]])
