(ns leihs.admin.resources.inventory-pools.inventory-pool.create
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [clojure.core.async :refer [<! go]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn create []
  (go (when-let [id (some->
                     {:url (path :inventory-pools)
                      :method :post
                      :json-params @data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (search-params/delete-from-url "action")
        (accountant/navigate!
         (path :inventory-pool {:inventory-pool-id id})))))

(defn form [& {:keys [is-editing]
               :or {is-editing false}}]
  [:div.inventory-pool.mt-3
   [:div.mb-3
    [form-components/switch-component data* [:is_active]
     :disabled (not @current-user/admin?*)
     :label "Active"]]
   [:div
    [form-components/input-component data* [:name]
     :label "Name"
     :required true]]
   [:div
    [form-components/input-component data* [:shortname]
     :label "Short name"
     :disabled is-editing
     :required true]]
   [:div
    [form-components/input-component data* [:email]
     :label "Email"
     :type :email
     :required true]]
   [form-components/input-component data* [:description]
    :label "Description"
    :element :textarea
    :rows 10]])

(def open*
  (reaction
   (reset! data* nil)
   (->> (:query-params @routing/state*)
        :action
        (= "add"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :show @open*}
   [:> Modal.Header {:closeButton true
                     :on-hide #(search-params/delete-from-url
                                "action")}
    [:> Modal.Title "Add Inventory Pool"]]
   [:> Modal.Body
    [:div.new-inventory-pool
     [:form.form
      {:id "create-inventory-pool-form"
       :on-submit (fn [e]
                    (.preventDefault e)
                    (create))}
      [form]]]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url
                            "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "create-inventory-pool-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [auth/admin-scopes?])
    [:<>
     [:> Button
      {:className "ml-3"
       :onClick #(search-params/append-to-url
                  {:action "add"})}
      "Add Inventory Pool"]]))
