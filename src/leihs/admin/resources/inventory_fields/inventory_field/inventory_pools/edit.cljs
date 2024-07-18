(ns leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-fields.inventory-field.core :as inventory-field.core]
   [leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.constants :as constants]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button Form Modal Alert]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn set-status! [pool-id enabled?]
  (swap! data*
         (fn [pools]
           (mapv (fn [pool]
                   (if (= pool-id (:id pool))
                     (assoc pool :is_disabled (not enabled?))
                     pool))
                 pools))))

(defn set-status-all! [enabled?]
  (swap! data*
         (fn [pools]
           (mapv (fn [pool]
                   (assoc pool :is_disabled (not enabled?)))
                 pools))))

(defn save []
  (let [route (path :inventory-field-inventory-pools (-> @routing/state* :route-params))]
    (go (when (some->
               {:url route
                :method :put
                :json-params @data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (search-params/delete-from-url "action")
          (reset! data* nil)
          (core/clean-and-fetch)))))

(defn status-component [enabled? pool-id]
  (let [switch-id (str pool-id "-switch")]
    [:div.custom-control.custom-switch
     [:input.custom-control-input
      {:id switch-id
       :name switch-id
       :type :checkbox
       :checked enabled?
       :on-change #(set-status! pool-id (.. % -target -checked))
       :tab-index constants/TAB-INDEX}]
     [:label.custom-control-label {:for switch-id}]]))

(defn status-component-all [enabled?]
  (let [switch-id "all-switch"]
    [:div.custom-control.custom-switch
     [:input.custom-control-input
      {:id switch-id
       :name switch-id
       :type :checkbox
       :checked enabled?
       :on-change #(set-status-all! (.. % -target -checked))
       :tab-index constants/TAB-INDEX}]
     [:label.custom-control-label {:for switch-id} "All"]]))

(defn core-table-component [pools]
  (if (seq pools)
    [table/container
     {:className "fields"
      :header [:tr
               [:th.align-top "Index"]
               [:th.align-top "Short name"]
               [:th.align-top "Inventory pool"]
               [:th.align-top
                [:div "Field status"]
                [:div.mt-2 [status-component-all (->> pools (every? :is_disabled) not)]]]]
      :body (doall
             (for [[index {:keys [id shortname name is_disabled]}] (map-indexed vector pools)]
               ^{:key id}
               [:tr
                [:td (inc index)]
                [:td shortname]
                [:td name]
                [:td
                 (let [enabled? (not is_disabled)]
                   [status-component enabled? id])]]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No pools found."]))

(defn form []
  (let [pools @data*]
    (if-not pools
      [wait-component]
      [:<>
       [:> Form {:id "inventory-pools-form"
                 :on-submit (fn [e]
                              (.preventDefault e)
                              (save))}
        [core-table-component pools]]])))

(def open?*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit-inventory-pools"))))

(defn dialog []
  [:> Modal {:size "lg"
             :scrollable true
             :centered true
             :show @open?*}
   [:> Modal.Header {:close-button true
                     :on-hide #(search-params/delete-from-url "action")}
    [:> Modal.Title "Edit Field Status"]]
   [:> Modal.Body [form]]
   [:> Modal.Footer
    [:> Button {:variant "secondary"
                :on-click #(search-params/delete-from-url "action")}
     "Cancel"]
    [:> Button {:type "submit"
                :form "inventory-pools-form"}
     "Save"]]])

(defn button []
  (let [required? (-> @inventory-field.core/inventory-field-data* :data :required)]
    [:<>
     [:> Button
      {:on-click #(search-params/append-to-url {:action "edit-inventory-pools"})
       :disabled required?}
      "Edit field status"]
     (when required? [:span.ml-2 "(not editable because required fields can not be disabled)"])]))
