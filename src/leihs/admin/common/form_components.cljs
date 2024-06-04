(ns leihs.admin.common.form-components
  (:refer-clojure :exclude [str])
  (:require [clojure.string :as string]
            [leihs.admin.common.icons :as icons]
            [leihs.core.constants :as constants]
            [leihs.core.core :refer [presence str]]
            [reagent.core :as reagent]))

(def TAB-INDEX constants/TAB-INDEX)

(defn set-value [value data* ks]
  (swap! data* assoc-in ks value)
  value)

(defn switch-component
  [data* ks & {:keys [disabled hint label
                      classes
                      key pre-change post-change
                      invalid-feedback]
               :or {disabled false
                    hint nil
                    classes []
                    label (last ks)
                    key (last ks)
                    pre-change identity
                    post-change identity}}]
  [:div.custom-control.custom-switch
   [:input.custom-control-input
    {:id key
     :class classes
     :type :checkbox
     :checked (boolean (get-in @data* ks))
     :on-change #(-> @data* (get-in ks) boolean not
                     pre-change
                     (set-value data* ks)
                     post-change)
     :tab-index TAB-INDEX
     :disabled disabled}]
   [:label.custom-control-label {:for key}
    (if (= label (last ks))
      [:strong label]
      [:span [:strong  label] [:small " (" [:span.text-monospace (last ks)] ")"]])]
   [:<> (when hint [:div [:small hint]])]
   [:<> (when invalid-feedback [:div.invalid-feedback invalid-feedback])]])

(defn checkbox-component
  [data* ks & {:keys [disabled hint label
                      classes
                      key pre-change post-change
                      invalid-feedback]
               :or {disabled false
                    hint nil
                    classes []
                    label (last ks)
                    key (last ks)
                    pre-change identity
                    post-change identity}}]
  [:div.form-check.form-check.mb-2
   [:input.form-check-input
    {:id key
     :class classes
     :type :checkbox
     :checked (boolean (get-in @data* ks))
     :on-change #(-> @data* (get-in ks) boolean not
                     pre-change
                     (set-value data* ks)
                     post-change)
     :tab-index TAB-INDEX
     :disabled disabled}]
   [:label.form-check-label {:for key}
    (if (= label (last ks))
      [:strong label]
      [:span [:strong  label] [:small " (" [:span.text-monospace (last ks)] ")"]])]
   [:<> (when hint [:div [:small hint]])]
   [:<> (when invalid-feedback [:div.invalid-feedback invalid-feedback])]])

(defn convert [value type]
  (when value
    (case type
      :number (int value)
      value)))

(defn input-component
  [data* ks & {:keys [label hint type element placeholder disabled rows required
                      on-change post-change
                      prepend append]
               :or {label (last ks)
                    hint nil
                    disabled false
                    required false
                    type :text
                    rows 10
                    element :input
                    on-change identity
                    post-change identity
                    prepend nil
                    append nil}}]
  [:div.form-group
   [:label {:for (last ks)}
    (if (= label (last ks))
      [:strong label]
      [:span [:strong  label] [:small " ("
                               [:span.text-monospace (last ks)] ")"]])]
   [:div.input-group
    (when prepend [prepend])
    [element
     {:id (last ks)
      :class :form-control
      :placeholder placeholder
      :type type
      :required required
      :value (get-in @data* ks)
      :on-change  #(-> % .-target .-value presence
                       (convert type)
                       on-change (set-value data* ks) post-change)
      :tab-index TAB-INDEX
      :disabled disabled
      :rows rows
      :auto-complete :off}]
    (when append [append])]
   (when hint [:small.form-text hint])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn submit-component
  [& {:keys [outer-classes btn-classes icon inner disabled]
      :or {outer-classes [:mb-3]
           btn-classes [:btn-danger]
           inner [:span "Just Do It"]
           icon [:i.fas.fa-question]
           disabled false}}]
  [:div
   {:class (->> outer-classes (map str) (string/join " "))}
   [:div.float-right
    [:button.btn
     {:class (->> btn-classes (map str) (string/join " "))
      :type :submit
      :disabled disabled
      :tab-index TAB-INDEX}
     icon " " inner]]
   [:div.clearfix]])

(defn save-submit-component [& args]
  [apply submit-component
   (concat [:btn-classes [:btn-warning]
            :icon [icons/save]
            :inner "Save"]
           args)])

;;;

(defn edit-modal-component [data header form-elements
                            & {:keys [abort-handler submit-handler]
                               :or {abort-handler #()
                                    submit-handler #()}}]
  (reagent/with-let [edit-data* (reagent/atom data)]
    [:div
     (let [changed? (not= data @edit-data*)]
       [:div.text-left {:style {:opacity "1.0" :z-index 10000}}
        [:div.modal {:style {:display "block" :z-index 10000}}
         [:div.modal-dialog
          [:div.modal-content
           [:div.modal-header [header]]
           [:div.modal-body
            [:form.form
             {:on-submit (fn [e]
                           (.preventDefault e)
                           (submit-handler @edit-data*))}
             [form-elements edit-data*]
             [:hr]
             [:div.row
              [:div.col
               (if changed?
                 [:button.btn.btn-outline-warning
                  {:type :button
                   :on-click abort-handler}
                  [icons/delete] " Cancel"]
                 [:button.btn.btn-outline-secondary
                  {:type :button
                   :on-click abort-handler}
                  [icons/delete] " Close"])]
              [:div.col
               [save-submit-component :disabled (not changed?)]]]]]]]]
        [:div.modal-backdrop {:style {:opacity "0.5"}}]])]))
