(ns leihs.admin.resources.inventory-pools.inventory-pool.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [clojure.core.async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :refer [Button Modal]]))

(defn create []
  (go (when-let [id (some->
                     {:url (path :inventory-pools)
                      :method :post
                      :json-params  @inventory-pool/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :inventory-pool {:inventory-pool-id id})))))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "lg"
             :centered true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add Inventory Pool"]]
   [:> Modal.Body
    [:div.new-inventory-pool
     [routing/hidden-state-component
      {:did-mount #(reset! inventory-pool/data* {})}]
     [:form.form
      {:id "create-inventory-pool-form"
       :on-submit (fn [e] (.preventDefault e) (create))}
      [inventory-pool/inventory-pool-form]]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "create-inventory-pool-form"}
     "Save"]]])

