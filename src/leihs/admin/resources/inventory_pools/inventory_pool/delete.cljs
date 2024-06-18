(ns leihs.admin.resources.inventory-pools.inventory-pool.delete
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :refer [Button Modal]]
   [reagent.core :refer [reaction]]))

(defn delete []
  (go (when (some->
             {:url (path :inventory-pool (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (search-params/delete-from-url "action")
        (accountant/navigate! (path :inventory-pools)))))

(def open*
  (reaction
   (->> (:query-params @routing/state*)
        :action
        (= "delete"))))

(defn dialog []
  [:> Modal {:size "sm"
             :centered true
             :show @open*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Delete Inventory Pool"]]
   [:> Modal.Body
    "Please confirm that you want to delete this inventory pool."]
   [:> Modal.Footer
    [:> Button {:onClick #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:variant "danger"
                :type "button"
                :on-click delete}
     "Delete"]]])

(defn button []
  (when (auth/allowed? [auth/admin-scopes?])
    [:<>
     [:> Button
      {:className "ml-3"
       :variant "danger"
       :on-click #(search-params/append-to-url {:action "delete"})}
      "Delete"]]))
