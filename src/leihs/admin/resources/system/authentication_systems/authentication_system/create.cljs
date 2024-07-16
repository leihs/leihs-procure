(ns leihs.admin.resources.system.authentication-systems.authentication-system.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(def data* (reagent/atom nil))

(defn create []
  (go (when-let [id (some->
                     {:chan (async/chan)
                      :url (path :authentication-systems)
                      :method :post
                      :json-params  @data*}
                     http-client/request :chan <!
                     http-client/filter-success! :body :id)]
        (accountant/navigate!
         (path :authentication-system
               {:authentication-system-id id})))))

(def open?*
  (reaction
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Add an Authentication System"]]
   [:> Modal.Body
    [core/form create data*]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "auth-form"}
     "Save"]]])

(defn button []
  [:> Button
   {:className "ml-3"
    :on-click #(search-params/append-to-url
                {:action "add"})}
   "Add Authentication System"])
