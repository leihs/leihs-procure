(ns leihs.admin.common.components.filter
  (:refer-clojure :exclude [str])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :refer [<! go timeout]]
   [clojure.core.match :refer [match]]
   [clojure.string :as string]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.constants :as constants]
   [leihs.core.core :refer [presence str]]
   [leihs.core.defaults :as defaults]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defn container [children]
  [:section.my-5.bg-light.p-3
   [:div.form-row
    children]])

(defn delayed-query-params-input-component
  [& {:keys [input-options query-params-key label prepend prepend-args classes]
      :or {input-options {}
           classes []
           query-params-key "replace-me"
           label "LABEL"
           prepend nil
           prepend-args []}}]
  (let [value* (reagent/atom "")]
    (fn [& _]
      [:div.form-group.my-2
       {:class (->> classes (map str) (string/join " "))}
       [routing/hidden-state-component
        {:did-change #(reset! value* (-> @routing/state* :query-params-raw query-params-key))}]
       [:label {:for query-params-key} [:span label [:small.text-monospace " (" query-params-key ")"]]]
       [:div.input-group
        (when prepend [apply prepend prepend-args])
        [:input.form-control
         (merge
          {:id query-params-key
           :value @value*
           :tab-index 1
           :placeholder query-params-key
           :on-change (fn [e]
                        (let [newval (or (some-> e .-target .-value presence) "")]
                          (reset! value* newval)
                          (go (<! (timeout 500))
                              (when (= @value* newval)
                                (accountant/navigate!
                                 (path (:handler-key @routing/state*)
                                       (:route-params @routing/state*)
                                       (merge {}
                                              (:query-params-raw @routing/state*)
                                              {:page 1
                                               query-params-key newval})))))))}
          input-options)]
        [:div.input-group-append
         [:button.btn.btn-secondary
          {:on-click (fn [_]
                       (reset! value* "")
                       (accountant/navigate!
                        (path (:handler-key @routing/state*)
                              (:route-params @routing/state*)
                              (merge {}
                                     (:query-params-raw @routing/state*)
                                     {:page 1
                                      query-params-key ""}))))}
          [icons/delete]]]]])))

(defn form-term-filter-component
  [& {:keys [label placeholder classes]
      :or {label "Search"
           placeholder "fuzzy term"
           classes [:col-md-3]}}]
  [delayed-query-params-input-component
   :label label
   :classes classes
   :query-params-key :term
   :input-options {:placeholder placeholder}
   :prepend nil])

(defn user-choose-prepend-component
  [& {:keys [text query-params-key] :or {text "Choose"}}]
  [:div.input-group-prepend
   [:a.btn.btn-primary
    {:tab-index constants/TAB-INDEX
     :href (path :users-choose {}
                 {:return-to (:url @routing/state*)
                  :query-params-key query-params-key})}
    [:span
     [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
     " " text " "]]])

(defn choose-user-component
  [& {:keys [input-options query-params-key label choose-text classes]
      :or {input-options {}
           query-params-key :user-uid
           label "User"
           choose-text "Choose"
           classes [:col-md-3]}}]
  [:div.col-md-4
   [delayed-query-params-input-component
    :label label
    :query-params-key query-params-key
    :input-options input-options
    :prepend user-choose-prepend-component
    :prepend-args [:text choose-text
                   :query-params-key query-params-key]]])

(defn select-component
  [& {:keys [options default-option query-params-key label classes]
      :or {label "Select"
           query-params-key :select
           classes []}}]
  (let [options (cond
                  (map? options) (->> options
                                      (map (fn [[k v]] [(str k) (str v)]))
                                      (into {}))
                  (sequential? options) (->> options
                                             (map (fn [x]
                                                    (match x
                                                      [k v] [(str k) (str v)]
                                                      :else [(str x) (str x)]))))
                  :else {"" ""})
        default-option (or default-option
                           (-> options first first))]
    [:div.form-group.m-2
     [:label {:for query-params-key}
      [:span label [:small.text-monospace " (" query-params-key ")"]]]
     [:div.input-group
      [:select.custom-select
       {:id query-params-key
        :value (let [val (get-in @routing/state* [:query-params-raw query-params-key])]
                 (if (some #{val} (map first options))
                   val
                   default-option))
        :on-change (fn [e]
                     (let [val (or (-> e .-target .-value presence) "")]
                       (accountant/navigate!
                        (path (:handler-key @routing/state*)
                              (:route-params @routing/state*)
                              (merge {}
                                     (:query-params-raw @routing/state*)
                                     {:page 1
                                      query-params-key val})))))}
       (for [[k n] options]
         [:option {:key k :value k} n])]
      [:div.input-group-append
       [:button.btn.btn-secondary
        {:on-click (fn [_]
                     (accountant/navigate!
                      (path (:handler-key @routing/state*)
                            (:route-params @routing/state*)
                            (merge {}
                                   (:query-params-raw @routing/state*)
                                   {:page 1
                                    query-params-key default-option}))))}
        [icons/delete]]]]]))

(defn form-per-page []
  [select-component
   :label "Per page"
   :query-params-key :per-page
   :options (map str defaults/PER-PAGE-VALUES)
   :default-option (str defaults/PER-PAGE)])

(defn form-including-user []
  [choose-user-component
   :query-params-key :including-user
   :input-options {:placeholder "email, login, or id"}])

(defn form-suspension []
  [select-component
   :label "Suspension"
   :query-params-key :suspension
   :options {"" "(suspended or not)"
             "suspended" "suspended"
             "unsuspended" "unsuspended"}])

(defn reset [& {:keys [default-query-params]
                :or {default-query-params nil}}]
  [:div.form-group.m-2
   [:label {:for :reset-query-params} "Filters"]
   [:div
    [:button#reset-query-params.btn.btn-secondary
     {:tab-index 1
      :on-click #(do (accountant/navigate!
                      (path (:handler-key @routing/state*)
                            (:route-params @routing/state*)
                            (if default-query-params
                              (merge (:query-params-raw @routing/state*)
                                     default-query-params)
                              {}))))}
     [:i.fas.fa-times]
     " Reset "]]])


