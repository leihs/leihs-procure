(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [clojure.set :refer [rename-keys]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(def data* (reagent/atom nil))

(defn create []
  (go (when-let [id (some->
                     {:chan (async/chan)
                      :url (path :inventory-pool-delegations
                                 {:inventory-pool-id @pool-core/id*})
                      :method :post
                      :json-params @data*}
                     http-client/request :chan <!
                     http-client/filter-success! :body :id)]
        (search-params/delete-from-url "action")
        (accountant/navigate!
         (path :inventory-pool-delegation {:inventory-pool-id @pool-core/id*
                                           :delegation-id id})))))

(def open*
  (reaction
   (core/merge-delegation-with-params data*)
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show @open*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-all-from-url)}
    [:> Modal.Title "Add a new Delegation"]]
   [:> Modal.Body
    [core/delegation-form create data*]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-all-from-url)}
     "Cancel"]
    [:> Button {:type "submit"
                :form "delegation-form"}
     "Add"]]])

(defn button []
  [:> Button
   {:className "ml-3"
    :on-click #(search-params/append-to-url
                {:action "add" :pool_protected true})}
   "Add Delegation"])
