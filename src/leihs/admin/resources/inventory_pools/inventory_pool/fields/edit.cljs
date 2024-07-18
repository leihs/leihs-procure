(ns leihs.admin.resources.inventory-pools.inventory-pool.fields.edit
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.fields.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.constants :as constants]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button Form Modal Alert]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce data* (reagent/atom nil))

(defn set-status! [field-id enabled?]
  (swap! data*
         (fn [fields]
           (mapv (fn [field]
                   (if (= field-id (:id field))
                     (assoc field :is_disabled (not enabled?))
                     field))
                 fields))))

(defn save []
  (let [route (path :inventory-pool-fields (-> @routing/state* :route-params))]
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

(defn status-component [enabled? field-id locked?]
  (let [switch-id (str field-id "-switch")]
    [:div.custom-control.custom-switch {:title (when locked? "This field can not be disabled because it is required")}
     [:input.custom-control-input
      {:id switch-id
       :name switch-id
       :type :checkbox
       :checked enabled?
       :disabled locked?
       :on-change #(set-status! field-id (.. % -target -checked))
       :tab-index constants/TAB-INDEX}]
     [:label.custom-control-label {:for switch-id}]]))

(defn core-table-component [fields]
  (if (seq fields)
    [table/container
     {:className "fields"
      :header [:tr
               [:th "Index"]
               [:th "ID"]
               [:th "Label"]
               [:th "Target Type"]
               [:th " "]
               [:th "Enabled?"]]
      :body (doall
             (for [[index {:keys [id data is_disabled]}] (map-indexed vector fields)]
               ^{:key id}
               [:tr.field
                [:td (inc index)]
                [:td id]
                [:td (-> data :label)]
                [:td (-> :data :target_type core/format-target-type)]
                [:td (when (-> data :required) [:span {:title "Required"} "*"])]
                [:td
                 (let [enabled? (not is_disabled)
                       locked? (-> data :required)]
                   [status-component enabled? id locked?])]]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No fields found."]))

(defn form []
  (let [fields @data*]
    (if-not fields
      [wait-component]
      [:<>
       [:> Form {:id "fields-form"
                 :on-submit (fn [e]
                              (.preventDefault e)
                              (save))}
        [core-table-component fields]]])))

(def open?*
  (reaction
   (reset! data* @core/data*)
   (->> (:query-params @routing/state*)
        :action
        (= "edit-fields"))))

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
                :form "fields-form"}
     "Save"]]])

(defn button []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    [:<>
     [:> Button
      {:on-click #(search-params/append-to-url
                   {:action "edit-fields"})}
      "Edit"]]))
