(ns leihs.admin.resources.inventory-fields.inventory-field.core
  (:require [cljs.core.async :as async :refer [<! go]]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as string]
            [com.rpl.specter :as specter]
            [leihs.admin.common.form-components :as form-components]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.inventory-fields.inventory-field.specs :as field-specs]
            [leihs.admin.state :as state]
            [leihs.core.core :refer [detect dissoc-in flip presence]]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]))

(defn select-put-keys [data]
  (-> data
      (select-keys field-specs/field-keys)
      (update :data select-keys (if (:dynamic data)
                                  field-specs/dynamic-field-data-keys
                                  field-specs/field-data-keys))))

(defn update-data-values-with-uuid [field]
  (cond-> field
    (some-> field :data :values count (> 0))
    (update-in [:data :values]
               #(vec (map (fn [v] (assoc v :uuid (random-uuid))) %)))))

(defn set-default-uuid [field]
  (cond-> field
    (#{"radio" "select"} (-> field :data :type))
    (assoc-in [:data :default-uuid]
              (:uuid (detect #(= (:value %) (-> field :data :default))
                             (-> field :data :values))))))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :inventory-field-id presence)
                ":inventory-field-id")))

(def new-dynamic-field-defaults field-specs/new-dynamic-field-defaults)
(def advanced-types field-specs/advanced-types)

(defonce cache* (reagent/atom nil))
(defonce path*
  (reaction (path :inventory-field {:inventory-field-id @id*})))

(defonce route-data*
  (reaction (get @cache* @path*)))

(defonce inventory-field-data*
  (reaction (:inventory-field-data @route-data*)))

(defonce inventory-field-usage-data*
  (reaction (:inventory-field-usage @route-data*)))

(defonce data*
  (reaction (-> @inventory-field-data*
                select-put-keys
                update-data-values-with-uuid
                set-default-uuid)))

(defonce inventory-fields-groups-data* (reagent/atom nil))

(defonce edit-mode?* (reagent/atom true))

(defn strip-of-uuids [data]
  (-> data
      (dissoc-in [:data :default-uuid])
      (cond->> (contains? (:data data) :values)
        (specter/transform [:data :values specter/ALL]
                           #(dissoc % :uuid)))))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

(defn fetch-inventory-fields-groups []
  (go (reset! inventory-fields-groups-data*
              (some-> {:chan (async/chan)
                       :url (path :inventory-fields-groups)}
                      http-client/request :chan <!
                      http-client/filter-success!
                      :body :inventory-fields-groups sort))))

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

(defn select-component [data* & {:keys [ks label values editable]
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

(defn select-type-component [data* & {:keys [disabled?]
                                      :or {disabled? false}}]
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
            :disabled (or used? disabled?)
            :value (or (get-in @data* ks) "")
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
                    [:option {:key (or (first v) "any"),
                              :value (first v)}
                     (second v)]))]]
         (when-not (-> @data* :data :values empty?)
           [:div.mt-3.ml-3
            [:strong.row
             [:div.col-2.text-center
              (when-not (-> @data* :data :type (= "checkbox")) "Default")]
             [:div.col-5 "Label shown in UI"]
             [:div.col-4 "Value saved in DB"]
             [:div.col-1]]
            (doall (for [option (-> @data* :data :values)]
                     [:div.row.my-3 {:key (:uuid option)}
                      [:div.col-2.text-center
                       (when-not (-> @data* :data :type (= "checkbox"))
                         [:input.align-middle
                          {:type :radio,
                           :checked (= (-> @data* :data :default-uuid) (:uuid option))
                           :on-change (fn [_]
                                        (set-value (:value option) data* [:data :default])
                                        (set-value (:uuid option) data* [:data :default-uuid]))
                           :disabled (not @edit-mode?*)}])]
                      [:div.col-5
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
               [:div.col-2]
               [:div.col-10 [:button.btn.btn-outline-secondary
                             {:type "button"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (swap! data*
                                                 update-in [:data :values]
                                                 (comp vec concat)
                                                 [(sorted-map :value nil :label nil :uuid (random-uuid))]))}
                             " + "]]])])]))))

(defn groups-radio-buttons-component [data*]
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

(defn input-attribute-component [data* & {:keys [disabled?]
                                          :or {disabled? false}}]
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
               :disabled (or disabled?
                             (and (:dynamic @inventory-field-data*)
                                  (> @inventory-field-usage-data* 0)))}]]
     [:div [:small (str "This attribute is used as the unique ID of the field and "
                        "identifies the respective portion of data stored in the items and/or licenses.")]]]))

(defn input-label-component [data*]
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
     [:div [:small "The label to be shown in the edit item/license form."]]]))

(defn checkbox-component [data* & {:keys [ks label]}]
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

(defn inventory-field-data-component [data*]
  [:<>
   [:strong "Data"]
   [:div.mt-1.mb-3
    [:code {:style {:white-space "pre-wrap"}}
     (-> @data* strip-of-uuids pprint with-out-str)]]])

(defn inventory-field-form-component [data*]
  [:<>
   (let [required-field? (-> @inventory-field-data* :data :required)]
     [:div.mb-3
      [form-components/checkbox-component data* [:active]
       :label "Active",
       :disabled (or (not @edit-mode?*) required-field?)]
      (when (and @edit-mode?* required-field?)
        [:div.alert.alert-info "This is a required field and thus cannot be deactivated."])])
   [input-label-component data*]])

(defn dynamic-inventory-field-form-component [data* & {:keys [isEditing?]
                                                       :or {isEditing? false}}]
  [:<>
   [:div.mb-4
    [form-components/checkbox-component data* [:active]
     :label "Active",
     :disabled (or (not @edit-mode?*)
                   (-> @inventory-field-data* :data :required))]]
   (-> @id* :dynamic boolean)
   (when (or (= @id* ":inventory-field-id")
             (:dynamic @inventory-field-data*))
     [input-attribute-component data* (when isEditing? {:disabled? true})])
   [input-label-component data*]
   [checkbox-component data*
    :label "Enabled for packages"
    :ks [:data :forPackage]
    :disabled (or (not @edit-mode?*)
                  (not (:dynamic @inventory-field-data*)))]
   [checkbox-component data*
    :label "Editable by owner only"
    :ks [:data :permissions :owner]
    :disabled (or (not @edit-mode?*)
                  (not (:dynamic @inventory-field-data*)))]
   [select-component data*
    :ks [:data :permissions :role]
    :label "Minimum role required for view"
    :values [["lending_manager" "lending_manager"]
             ["inventory_manager" "inventory_manager"]]]
   [groups-radio-buttons-component data*]
   [select-component data*
    :ks [:data :target_type]
    :label "Target"
    :values [["" "Item+License"]
             ["item" "Item"]
             ["license" "License"]]]
   [select-type-component data*]])

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.field-debug
     [:hr]
     [:div.field-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div.field-data
      [:h3 "@inventory-field-data*"]
      [:pre (with-out-str (pprint @inventory-field-data*))]]
     [:div.inventory-fields-groups-data
      [:h3 "@inventory-field-usage-data*"]
      [:pre (with-out-str (pprint @inventory-field-usage-data*))]]
     [:div.inventory-fields-groups-data
      [:h3 "@inventory-fields-groups-data*"]
      [:pre (with-out-str (pprint @inventory-fields-groups-data*))]]]))
