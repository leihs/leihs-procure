(ns leihs.admin.resources.inventory-pools.inventory-pool.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [clojure.core.async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [react-bootstrap :refer [Button Modal]]))

(defn patch []
  (let [route (path :inventory-pool
                    {:inventory-pool-id @inventory-pool/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @inventory-pool/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "lg"
             :centered true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Inventory Pool"]]
   [:> Modal.Body
    [inventory-pool/inventory-pool-form {:is-editing true}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:onClick #(do (patch) (onHide))}
     "Save"]]])

