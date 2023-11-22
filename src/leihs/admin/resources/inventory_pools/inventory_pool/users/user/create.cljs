(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.create
  (:refer-clojure :exclude [str keyword])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.users.user.edit :as edit]
   [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
   [react-bootstrap :as react-bootstrap :refer [Button Form Modal]]))

(defn post []
  (go (when-let [id (some->
                     {:chan (async/chan)
                      :url (path :users)
                      :method :post
                      :json-params  (-> @data*
                                        (update-in
                                         [:extended_info]
                                         (fn [s] (.parse js/JSON s))))}
                     http-client/request :chan <!
                     http-client/filter-success! :body :id)]
        (accountant/navigate!
         (path :inventory-pool-user
               {:inventory-pool-id @inventory-pool/id* :user-id id})))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "xl"
             :centered true
             :scrollable true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Add a new User"]]
   [:> Modal.Body
    [:> Form {:id "add-user-to-inventory-pool-form"
              :on-submit (fn [e] (.preventDefault e) (post))}
     [edit/inner-form-component]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:type "submit"
                :form "add-user-to-inventory-pool-form"}
     "Add"]]])
