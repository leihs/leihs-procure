(ns leihs.admin.resources.inventory-pools.inventory-pool.edit
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [clojure.core.async :refer [<! go]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.user.front :as current-user]
   [react-bootstrap :refer [Button Modal]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn patch []
  (let [route (path :inventory-pool
                    {:inventory-pool-id @core/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params @data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn form [& {:keys [is-editing]
               :or {is-editing false}}]
  (if-not @data*
    [wait-component]
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
      :rows 10]
     [form-components/input-component data* [:default_contract_note]
      :label "Default Contract Note"
      :element :textarea
      :rows 5]
     [:div.mb-3
      [form-components/switch-component data* [:print_contracts]
       :label "Print Contracts"]]
     [:div.mb-3
      [form-components/switch-component data* [:automatic_suspension]
       :label "Automatic Suspension"]]
     (when (:automatic_suspension @data*)
       [form-components/input-component data* [:automatic_suspension_reason]
        :label "Automatic Suspension Reason"
        :element :textarea
        :rows 5])
     [:div.mb-3
      [form-components/switch-component data* [:required_purpose]
       :label "Hand Over Purpose"]]
     [:div.mb-3
      [form-components/input-component data* [:reservation_advance_days]
       :label "Reservation Advance Days"
       :type :number
       :min 0]]]))

(defn dialog [& {:keys [show onHide] :or {show false}}]
  [:> Modal {:size "lg"
             :centered true
             :show show}
   [:> Modal.Header {:closeButton true
                     :onHide onHide}
    [:> Modal.Title "Edit Inventory Pool"]]
   [:> Modal.Body
    [form {:is-editing true}]]
   [:> Modal.Footer
    [:> Button {:variant "secondary" :onClick onHide}
     "Cancel"]
    [:> Button {:onClick #(do (patch) (onHide))}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    (let [show (reagent/atom false)]
      (fn []
        [:<>
         [:> Button
          {:className ""
           :onClick #(do (when-not @data* (reset! data* @core/data*))
                         (reset! show true))}
          "Edit"]
         [dialog {:show @show
                  :onHide #(reset! show false)}]]))))
