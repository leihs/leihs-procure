(ns leihs.admin.resources.inventory-fields.inventory-field.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [dissoc-in keyword str presence flip drop-at]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-fields.breadcrumbs :as breadcrumbs-parent]
    [leihs.admin.resources.inventory-fields.inventory-field.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-fields.inventory-field.core :as inventory-field
     :refer [clean-and-fetch clean-and-fetch-for-new
             id* data* edit-mode?*
             inventory-field-data*
             inventory-field-usage-data*
             inventory-fields-groups-data*
             advanced-types
             new-dynamic-field-defaults]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.core.log.helpers :refer [spy log]]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [clojure.string :as string]
    [com.rpl.specter :as specter]
    [reagent.core :as reagent]
    ))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-value [value data* ks]
  (swap! data* assoc-in ks value)
  value)

(defn get-id-from-ks [ks]
  (->> ks (map name) (string/join ":")))

(defn update-data-value [field uuid update-fn]
  (specter/transform [:data :values specter/ALL #(-> % :uuid (= uuid))]
                     update-fn
                     field))

(comment (specter/select [:data :values specter/ALL #(-> % :value (= "none"))]
                         ; #(assoc % :label "foo")
                         @inventory-field-data*))

(defn strip-of-uuids [data]
  (-> data
      (dissoc-in [:data :default-uuid])
      (cond->> 
        (contains? (:data data) :values)
        (specter/transform [:data :values specter/ALL]
                           #(dissoc % :uuid)))))

; (defn select-required-component []
;   (let [id "data:required", ks [:data :required]]
;     [:div.form-group
;      [:label {:for id}
;       [:span [:strong "Mandatory"] [:small " (" [:span.text-monospace id] ")"]]]
;      [:div.input-group {:id id}
;       [:select.custom-select
;        {:id id
;         :disabled true
;         :value (case (get-in @inventory-field-data* ks), true "yes", false "no")}
;        [:option {:key "yes" :value "yes"} "yes"]
;        [:option {:key "no" :value "no"} "no"]]]]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-component [& {:keys [ks label values editable]
                           :or {editable @edit-mode?*}}]
  (let [id (get-id-from-ks ks)]
    [:div.mt-3.form-group
     [:label {:for id}
      [:span [:strong label] [:small " (" [:span.text-monospace id] ")"]]]
     [:div.input-group {:id id}
      [:select.custom-select
       {:id id
        :disabled (not editable)
        :value (or (get-in @data* ks) "")
        :on-change (fn [e]
                     (let [val (-> e .-target .-value presence)]
                       (set-value val data* ks)))}
       (doall (for [v values]
                [:option {:key (or (first v) (random-uuid))
                          :value (first v)}
                 (second v)]))]]]))

(defn select-type-component []
  (let [old-values (atom (->> @data* :data :values (map :value)))]
    (fn []
      (let [ks [:data :type],
            id (get-id-from-ks ks),
            label "Type",
            used? (> @inventory-field-usage-data* 0)]
        [:div.mt-3.form-group
         [:label {:for id}
          [:span [:strong label] [:small " (" [:span.text-monospace id] ")"]]]
         [:div.input-group {:id id}
          [:select.custom-select
           {:id id
            :disabled (or (not @edit-mode?*) used?)
            :value (get-in @data* ks)
            :on-change (fn [e]
                         (let [val (-> e .-target .-value presence)]
                           (set-value val data* ks)
                           (reset! old-values nil)
                           (if (and (advanced-types val) (-> data* :data :values empty?))
                             (let [uuid (random-uuid)] 
                               (do (swap! data*
                                          update-in [:data :values]
                                          (constantly [(sorted-map :value nil :label nil :uuid uuid)]))
                                   (when (not= val "checkbox")
                                     (swap! data*
                                            update-in [:data :default-uuid]
                                            (constantly uuid)))))
                             (swap! data* dissoc-in [:data :values]))))}
           (doall (for [v [["checkbox" "Checkbox"]
                           ["date" "Date"]
                           ["radio" "Radio"]
                           ["select" "Select"]
                           ["text" "Text"]
                           ["textarea" "Textarea"]]]
                    [:option {:key (or (first v) "any"), :value (first v)}
                     (second v)]))]]
         (when-not (-> @data* :data :values empty?)
           [:div.mt-3.ml-3
            [:strong.row
             [:div.col-1 (when-not (-> @data* :data :type (= "checkbox")) "Default")]
             [:div.col-6 "Label"]
             [:div.col-4 "Value"]
             [:div.col-1]]
            (doall (for [option (-> @data* :data :values)]
                     [:div.row.my-3 {:key (:uuid option)}
                      [:div.col-1.text-center
                       (when-not (-> @data* :data :type (= "checkbox"))
                         [:input.align-middle
                          {:type :radio,
                           :checked (= (-> @data* :data :default-uuid) (:uuid option))
                           :on-change (fn [_]
                                        (set-value (:value option) data* [:data :default])
                                        (set-value (:uuid option) data* [:data :default-uuid]))
                           :disabled (not @edit-mode?*)}])]
                      [:div.col-6
                       [:input.form-control {:type :text, :value (:label option),
                                             :on-change (fn [e]
                                                          (let [val (-> e .-target .-value presence)]
                                                            (swap! data*
                                                                   (fn [data]
                                                                     (update-data-value data (:uuid option)
                                                                                        #(assoc % :label val))))))
                                             :required true
                                             :disabled (not @edit-mode?*)}]]
                      [:div.col-4
                       [:input.form-control {:type :text :value (:value option)
                                             :on-change (fn [e]
                                                          (let [val (-> e .-target .-value presence)]
                                                            (swap! data*
                                                                   (fn [data]
                                                                     (update-data-value data (:uuid option)
                                                                                        #(assoc % :value val))))
                                                            (when (= (-> @data* :data :default-uuid) (:uuid option))
                                                              (swap! data* assoc-in [:data :default] val))))
                                             :required true
                                             :disabled (or (not @edit-mode?*)
                                                           (some #{(:value option)} @old-values))}]]
                      (when-not (or (not @edit-mode?*)
                                    (some #{(:value option)} @old-values))
                        [:div.col-1
                         (when-not (or (not @edit-mode?*)
                                       (some #{(:value option)} @old-values)
                                       (-> @data* :data :values count (= 1)))
                           [:button.btn.btn-outline-secondary
                            {:type "button"
                             :on-click (fn [e]
                                         (.preventDefault e)
                                         (swap! data*
                                                update-in [:data :values]
                                                (flip remove) #(-> % :uuid (= (:uuid option)))))}
                            " - "])])]))
            (when @edit-mode?*
              [:div.row
               [:div.col-1]
               [:div.col-11 [:button.btn.btn-outline-secondary
                             {:type "button"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (swap! data*
                                                 update-in [:data :values]
                                                 (comp vec concat)
                                                 [(sorted-map :value nil :label nil :uuid (random-uuid))]))}
                             " + "]]])])]))))

(defn groups-radio-buttons-component []
  (let [id "data:group", ks [:data :group],
        custom-group-label (reagent/atom "")
        custom-group-checked? (reagent/atom false)]
    (fn []
      [:div.form-group
       [:label {:for id}
        [:span [:strong "Field-Group"]
         [:small " (" [:span.text-monospace id] ")"]]]
       [:div {:id id}
        (let [field-group "none"]
          [:div.form-check.mb-2
           [:input.form-check-input.mt-2 {:type :radio
                                          :name field-group
                                          :id field-group
                                          :on-change (fn [e]
                                                       (reset! custom-group-checked? false)
                                                       (set-value nil data* ks))
                                          :checked (nil? (get-in @data* ks))
                                          :disabled (not @edit-mode?*)}]
           [:label.form-check-label {:for field-group} [:i (str "<" field-group ">")]]])
        (doall
          (for [field-group @inventory-fields-groups-data*]
            [:div.form-check.mb-2 {:key field-group}
             [:input.form-check-input {:type :radio
                                       :name field-group
                                       :id field-group
                                       :on-change (fn [e]
                                                    (let [val (-> e .-target .-name presence)]
                                                      (reset! custom-group-checked? false)
                                                      (set-value val data* ks)))
                                       :checked (= (get-in @data* ks) field-group)
                                       :disabled (not @edit-mode?*)}]
             [:label.form-check-label {:for field-group} field-group]]))
        (let [field-group "Custom"]
          [:div.form-check
           [:input.form-check-input.mt-2 {:type :radio
                                          :name field-group
                                          :id field-group
                                          :on-change (fn [_e]
                                                       (reset! custom-group-checked? true)
                                                       (set-value @custom-group-label data* ks))
                                          :checked @custom-group-checked?
                                          :disabled (not @edit-mode?*)}]
           [:input.form-control {:type :text,
                                 :on-change (fn [e]
                                              (reset! custom-group-label (-> e .-target .-value))
                                              (when @custom-group-checked?
                                                (set-value @custom-group-label data* ks)))
                                 :value @custom-group-label}]])]])))

(defn input-attribute-component []
  (let [id "data:attribute", ks [:data :attribute]]
    [:div.form-group
     [:label {:for id}
      [:span [:strong "Unique ID-Attribute"]
       [:small " (" [:span.text-monospace id] ")"]]]
     [:div.input-group
      [:div.input-group-prepend [:span.input-group-text "properties_"]]
      [:input {:id id
               :class :form-control
               :type :text
               :value (second (get-in @data* ks))
               :on-change (fn [e]
                            (let [val (or (-> e .-target .-value presence) "")]
                              (set-value val data* (conj ks 1))))
               :required true
               :disabled (or (not @edit-mode?*)
                             (and (:dynamic @inventory-field-data*)
                                  (> @inventory-field-usage-data* 0)))}]]
     [:div [:small (str "This attribute is used as the unique ID of the field and "
                        "identifies the respective portion of data stored in the items and/or licenses.")]]]))

(defn input-label-component []
  (let [id "data:label", ks [:data :label]]
    [:div.form-group
     [:label {:for id}
      [:span [:strong "Label"]
       [:small " (" [:span.text-monospace id] ")"]]]
     [:div.input-group
      [:input {:id id
               :class :form-control
               :type :text
               :value (get-in @data* ks)
               :on-change (fn [e]
                            (let [val (or (-> e .-target .-value presence) "")]
                              (set-value val data* ks)))
               :required true
               :disabled (not @edit-mode?*)}]]
     [:div [:small "The label to be shown in the edit item/license form."] ]]))

(defn checkbox-component [& {:keys [ks label]}]
  (let [id (get-id-from-ks ks)]
    [:div.form-check.form-check.mb-2
     [:input.form-check-input
      {:id id
       :type :checkbox
       :checked (boolean (get-in @data* ks))
       :on-change #(-> @data* (get-in ks) boolean not
                       (set-value data* ks))
       :tab-index 1
       :disabled (not @edit-mode?*)}]
     [:label.form-check-label {:for id}
      [:span [:strong label] [:small " (" [:span.text-monospace id] ")"]]]]))

(defn inventory-field-data-component []
  [:<>
   [:strong "Data"]
   [:div.mt-1.mb-3
    [:code {:style {:white-space "pre-wrap"}}
     (-> @data* strip-of-uuids pprint with-out-str)]]])

(defn core-inventory-field-form-component []
  [:<>
   (let [required-field? (-> @inventory-field-data* :data :required)]
     [:div.mb-3
      [form-components/checkbox-component data* [:active]
       :label "Active",
       :disabled (or (not @edit-mode?*) required-field?)]
      (when (and @edit-mode?* required-field?)
        [:div.alert.alert-info "This is a required field and thus cannot be deactivated."])])
   [input-label-component]])

(defn dynamic-inventory-field-form-component []
  [:<>
   [:div.mb-4
    [form-components/checkbox-component data* [:active]
     :label "Active",
     :disabled (or (not @edit-mode?*)
                   (-> @inventory-field-data* :data :required))]]
   (-> @id* :dynamic boolean)
   (when (or (= @id* ":inventory-field-id") (:dynamic @inventory-field-data*))
     [input-attribute-component])
   [input-label-component]
   [checkbox-component
    :label "Enabled for packages"
    :ks [:data :forPackage]
    :disabled (or (not @edit-mode?*)
                  (not (:dynamic @inventory-field-data*)))]
   [checkbox-component
    :label "Editable by owner only"
    :ks [:data :permissions :owner]
    :disabled (or (not @edit-mode?*)
                  (not (:dynamic @inventory-field-data*)))]
   [select-component
    :ks [:data :permissions :role]
    :label "Minimum role required for view"
    :values [["lending_manager" "lending_manager"]
             ["inventory_manager" "inventory_manager"]]]
   [groups-radio-buttons-component]
   [select-component
    :ks [:data :target_type]
    :label "Target"
    :values [["" "Item+License"]
             ["item" "Item"]
             ["license" "License"]]]
   [select-type-component]])

(defn edit-inventory-field-component []
  (if-not @data*
    [wait-component]
    [:div.inventory-field.mt-3
     (if-not (:dynamic @inventory-field-data*)
       [:div.alert.alert-info
        (str "Some of the attributes of this inventory field cannot be edited "
             "because it belongs the the core group of inventory fields of the system.")]
       [:ul.list-unstyled
        [:li
         [:dl.row.mb-0
          [:dt.col-6 {:style {:max-width "80px"}}
           [:span "Usage:"]]
          [:dd.col-6 @inventory-field-usage-data*
           " Items/Licenses"]]]])
     [:div.row
      [:div.col-md
       (if (:dynamic @inventory-field-data*)
         [dynamic-inventory-field-form-component]
         [core-inventory-field-form-component])]
      [:div.col-md
       [inventory-field-data-component]]]]))

(defn create-inventory-field-component []
  (if-not @data*
    [wait-component]
    [:div.inventory-field.mt-3
     [:div.row
      [:div.col-md
       [dynamic-inventory-field-form-component]]
      [:div.col-md
       [inventory-field-data-component]]]]))

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment (specter/transform [:data :values specter/ALL]
                            #(dissoc % :uuid)
                            {:foo 1} #_@data*))

(defn patch [& args]
  (let [route (path :inventory-field {:inventory-field-id @id*})]
  (go (when (some->
              {:url route
               :method :patch
               :json-params (strip-of-uuids @data*)
               :chan (async/chan)}
              http-client/request :chan <!
              http-client/filter-success!)
        (accountant/navigate! route)))))

(defn edit-page []
  [:div.edit-inventory-field
   [routing/hidden-state-component {:did-mount clean-and-fetch}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs/left*
           [breadcrumbs/edit-li])[])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Inventory-Field "]
      [inventory-field/id-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [edit-inventory-field-component]
    [form-components/save-submit-component]]
   [inventory-field/debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create []
  (go (when-let [id (some->
                      {:url (path :inventory-fields)
                       :method :post
                       :json-params (strip-of-uuids @data*)
                       :chan (async/chan)}
                      http-client/request :chan <!
                      http-client/filter-success!
                      :body :id)]
        (accountant/navigate!
          (path :inventory-field {:inventory-field-id id})))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div.mb-3
     [:div.float-right
      [:button.btn.btn-primary
       " Create "]]
     [:div.clearfix]]))

(defn new-id-component []
  (let [id-second-part (-> @data* :data :attribute second presence
                           (or "..."))]
    [:i [:span.text-info (str "properties_" id-second-part)]]))

(defn create-page []
  [:div.new-inventory-field
   [routing/hidden-state-component
    {:did-mount clean-and-fetch-for-new}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs-parent/left*
           [breadcrumbs-parent/create-li])
     [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Inventory-Field "]
      [new-id-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (create))}
    [create-inventory-field-component]
    [create-submit-component]]
   [inventory-field/debug-component]])

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-inventory-field [& args]
  (go (when (some->
              {:url (path :inventory-field (-> @routing/state* :route-params))
               :method :delete
               :chan (async/chan)}
              http-client/request :chan <!
              http-client/filter-success!)
        (accountant/navigate! (path :inventory-fields)))))

(defn delete-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-inventory-field))}
   [form-components/delete-submit-component]])

(defn delete-page []
  [:div.group-delete
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Inventory-Field "
    [inventory-field/id-link-component]]
   [inventory-field/id-component]
   [delete-form-component]
   [inventory-field/debug-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.inventory-field.mb-5
   [routing/hidden-state-component {:did-mount clean-and-fetch}]
   [breadcrumbs/nav-component
     @breadcrumbs/left*
     [[breadcrumbs/inventory-fields-li]
      (when (and (:dynamic @inventory-field-data*)
                 (-> @inventory-field-data* :data :required not)
                 (= @inventory-field-usage-data* 0))
        [breadcrumbs/delete-li])
      [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Inventory-Field "]
      [inventory-field/id-link-component]]]]
   [edit-inventory-field-component]
   [inventory-field/debug-component]])
