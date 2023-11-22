(ns leihs.admin.resources.inventory-pools.inventory-pool.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :refer [Button Modal]]))

(defn delete []
  (go (when (some->
             {:url (path :inventory-pool (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate! (path :inventory-pools)))))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "sm"
             :centered true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Delete Inventory Pool"]]
   [:> Modal.Body
    "Please confirm that you want to delete this inventory pool."]
   [:> Modal.Footer
    [:> Button {:onClick onHide}
     "Cancel"]
    [:> Button {:variant "danger"
                :type "button"
                :onClick delete}
     "Delete"]]])
