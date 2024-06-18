(ns leihs.admin.resources.inventory-pools.inventory-pool.workdays.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [clojure.string :refer [capitalize]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button Form Modal]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn patch []
  (let [workdays-route (path :inventory-pool-workdays
                             {:inventory-pool-id (:inventory_pool_id @data*)})]
    (go (when (some->
               {:url workdays-route
                :method :patch
                :json-params @data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (reset! core/data* @data*)
          (search-params/delete-from-url "action")))))

(defn opened-closed-comp [day]
  (let [switch-id (str (name day) "-switch")]
    [:div.custom-control.custom-switch
     [:input.custom-control-input
      {:id switch-id
       :name (name day)
       :type :checkbox
       :checked (day @data*)
       :on-change #(swap! data* update day not)
       :tab-index constants/TAB-INDEX}]
     [:label.custom-control-label {:for switch-id}]]))

(defn max-visits-comp [day]
  [:div.input-group
   [:input.form-control
    {:type "number"
     :min 1
     :value ((core/DAYS day) (:max_visits @data*))
     :placeholder "unlimited"
     :on-change #(swap! data*
                        assoc-in
                        [:max_visits (core/DAYS day)]
                        (-> % .-target .-value presence))}]])

(defn form []
  (if-not data*
    [wait-component]
    [:> Form {:id "workdays-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (patch))}
     [table/container
      {:borders false
       :header [:tr [:th "Day"] [:th "Open/Closed"] [:th "Max. Allowed Visits"]]
       :body (doall (for [day (keys core/DAYS)]
                      [:tr {:key (name day)}
                       [:td (capitalize (name day))]
                       [:td [opened-closed-comp day]]
                       [:td [max-visits-comp day]]]))}]]))

(def open*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit-workdays"))))

(defn dialog []
  [:> Modal {:size "lg"
             :centered true
             :show @open*}
   [:> Modal.Header {:closeButton true
                     :onHide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Edit Workdays"]]
   [:> Modal.Body
    [form]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "workdays-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed?
         [pool-auth/pool-inventory-manager?
          auth/admin-scopes?])
    [:<>
     [:> Button
      {:on-click #(search-params/append-to-url
                   {:action "edit-workdays"})}
      "Edit"]]))
