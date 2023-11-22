(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.shared :as shared]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [taoensso.timbre]))

(defn create [data]
  (go (when-let [id (some->
                     {:chan (async/chan)
                      :url (path :inventory-pool-delegations
                                 {:inventory-pool-id @inventory-pool/id*})
                      :method :post
                      :json-params data}
                     http-client/request :chan <!
                     http-client/filter-success! :body :id)]
        (accountant/navigate!
         (path :inventory-pool-delegation {:inventory-pool-id @inventory-pool/id*
                                           :delegation-id id})))))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a new Delegation"]]
   [:> Modal.Body
    [shared/delegation-form {:action create
                             :id "add-delegation-form"}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "add-delegation-form"}
     "Add"]]])
