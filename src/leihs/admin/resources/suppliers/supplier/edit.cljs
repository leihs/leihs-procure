(ns leihs.admin.resources.suppliers.supplier.edit
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as supplier-core]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]))

(defn patch []
  (let [route (path :supplier {:supplier-id @supplier-core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @supplier-core/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Supplier"]]
   [:> Modal.Body
    [supplier-core/form patch]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "supplier-form"}
     "Save"]]])

