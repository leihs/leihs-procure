(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.edit
  (:require
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

(defn patch []
  (let [route (path :inventory-pool-delegation
                    {:inventory-pool-id @pool-core/id*
                     :delegation-id @core/id*})]
    (go (when (some->
               {:chan (async/chan)
                :url route
                :method :patch
                :json-params  @data*}
               http-client/request :chan <!
               http-client/filter-success!)
          (reset! core/data* @data*)
          (search-params/delete-all-from-url)))))

(def open*
  (reaction
   (core/merge-delegation-with-params data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show @open*}
   [:> Modal.Header {:closeButton true
                     :onHide #(search-params/delete-all-from-url)}
    [:> Modal.Title "Edit Delegation"]]
   [:> Modal.Body
    [core/delegation-form patch data*]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-all-from-url)}
     "Cancel"]
    [:> Button {:type "submit"
                :form "delegation-form"}
     "Save"]]])

(defn button []
  [:> Button
   {:on-click #(search-params/append-to-url
                {:action "edit"})}
   "Edit"])
