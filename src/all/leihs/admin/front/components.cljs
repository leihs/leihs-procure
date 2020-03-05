(ns leihs.admin.front.components
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [cljs.pprint :refer [pprint]]
    ))

(defn pre-component [data]
  [:pre (with-out-str (pprint data))])


(defn field-component
  ([ks state-atom edit-mode?]
   (field-component ks state-atom edit-mode? {}))
  ([ks state-atom edit-mode? opts]
   (let [opts (merge {:type :text :node-type :input} opts)
         kw (last ks)]
     [:div.form-group.row
      [:label.col.col-form-label.col-sm-2 {:for kw} kw]
      [:div.col.col-sm-10
       [:div.input-group
        (if @edit-mode?
          [(:node-type opts)
           {:class :form-control
            :id kw
            :type (:type opts)
            :value (get-in @state-atom ks "")
            :on-change #(swap! state-atom assoc-in ks (-> % .-target .-value presence))
            :disabled (not @edit-mode?)}]
          [(if (= (:node-type opts) :textarea) :pre :div)
           (if-let [value (get-in @state-atom ks nil)]
             [:span.form-control-plaintext
              (case (:type opts)
                :email [:a {:href (str "mailto:" value)}
                        [:i.fas.fa-envelope] " " value]
                :url [:a {:href value} value]
                value)])])]]])))

(defn checkbox-component [ks state edit-mode?]
  (let [kw (last ks)]
    [:div.form-group.row
     [:div.col.col-form-label.col-sm-2 {:for kw} kw]
     [:div.col-sm-10
      [:div.form-check.form-check-inline
       [:input
        {:id kw
         :type :checkbox
         :checked (-> @state (get-in ks) boolean)
         :value nil
         :on-change #(swap! state assoc-in ks (-> @state (get-in ks) boolean not))
         :disabled (not @edit-mode?)
         :readOnly (not @edit-mode?)}]]]]))
