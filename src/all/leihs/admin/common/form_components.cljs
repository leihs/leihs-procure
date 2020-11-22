(ns leihs.admin.common.form-components
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.utils.misc :as front-shared :refer [gravatar-url wait-component]]
    [leihs.core.icons :as icons]
    [cljs.pprint :refer [pprint]]
    ))

(def TAB-INDEX 1)

(defn checkbox-component
  [data* ks & {:keys [disabled hint label] :or
               {disabled false
                hint nil
                label (last ks)}}]
  [:div.form-check.form-check
   [:input.form-check-input
    {:id (last ks)
     :type :checkbox
     :checked (boolean (get-in @data* ks))
     :on-change #(swap! data* assoc-in ks (-> @data* (get-in ks) boolean not))
     :tab-index TAB-INDEX
     :disabled disabled}]
   [:label.form-check-label {:for (last ks)}
    (if (= label (last ks))
      [:strong label]
      [:span [:strong  label] [:small " (" [:span.text-monospace (last ks)] ")"]])]
   (when hint [:p [:small hint]]) ])

(defn set-value [value data* ks]
  (swap! data* assoc-in ks value)
  value)

(defn input-component
  [data* ks & {:keys [label hint type element placeholder disabled rows
                      on-change post-change
                      prepend append
                      ]
               :or {label (last ks)
                    hint nil
                    disabled false
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
      :value (get-in @data* ks)
      :on-change  #(-> % .-target .-value
                       on-change (set-value data* ks) post-change)
      :tab-index TAB-INDEX
      :disabled disabled
      :rows rows
      :auto-complete :off}]
    (when append [append])]
   (when hint [:small hint])])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn submit-component
  [& {:keys [btn-class icon inner]
      :or {btn-class "btn-danger"
           inner [:span "Just Do It"]
           icon [:i.fas.fa-question]}}]
  [:div.mb-3
   [:div.float-right
    [:button.btn.btn-warning
     {:class btn-class
      :type :submit
      :tab-index TAB-INDEX}
     icon " " inner]]
   [:div.clearfix]])

(defn create-submit-component []
  [submit-component
   :btn-class :btn-primary
   :icon icons/add
   :inner "Create"])

(defn delete-submit-component []
  [submit-component
   :btn-class :btn-danger
   :icon icons/delete
   :inner "Delete"])

(defn save-submit-component []
  [submit-component
   :btn-class :btn-warning
   :icon icons/save
   :inner "Save"])

(defn small-add-submit-component []
  [:button.btn.btn-primary.btn-sm
   {:type :submit}
   [:span icons/add " Add "]])

(defn small-remove-submit-component []
  [:button.btn.btn-warning.btn-sm
   {:type :submit}
   [:span icons/delete " Remove "]])
