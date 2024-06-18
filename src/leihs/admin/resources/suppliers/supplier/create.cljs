(ns leihs.admin.resources.suppliers.supplier.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as supplier-core]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(def data* (reagent/atom nil))

(defn create []
  (go (when-let [id (some->
                     {:url (path :suppliers)
                      :method :post
                      :json-params  @data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (search-params/delete-from-url "action")
        (accountant/navigate!
         (path :supplier {:supplier-id id})))))

(def open?*
  (reaction
   (reset! data* nil)
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog [& {:keys [show onHide]
                 :or {show false}}]
  [:> Modal {:size "md"
             :centered true
             :scrollable true
             :show @open?*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Add a Supplier"]]
   [:> Modal.Body
    [supplier-core/form create data*]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "supplier-form"}
     "Save"]]])

(defn button []
  [:> Button
   {:className "ml-3"
    :on-click #(search-params/append-to-url
                {:action "add"})}
   "Add Supplier"])
