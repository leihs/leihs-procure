(ns leihs.admin.resources.settings.languages.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request
                      :chan <! http-client/filter-success :body))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn form [action]
  [:> Form
   {:id "languages-form"
    :on-submit (fn [e]
                 (.preventDefault e)
                 (action))}
   [table/container
    {:header [:tr
              [:th "Locale"]
              [:th "Name"]
              [:th "Active"]
              [:th "Default"]]
     :body
     (doall
      (for [[locale lang] @data*]
        ^{:key locale}
        [:tr
         [:td (:locale lang)]
         [:td [:span (:name lang)]]
         [:td.active
          [form-components/checkbox-component
           data* [locale :active]
           :key (str locale "-" :active)
           :disabled (get-in @data* [locale :default])]]
         [:td.default
          [form-components/checkbox-component data*
           [locale :default]
           :key (str locale "-" :default)
           :disabled (or (-> @data* (get-in [locale :active]) not)
                         (get-in @data* [locale :default]))
           :pre-change (fn [v]
                         (doseq [locale (keys @data*)]
                           (swap! data* assoc-in [locale :default] false))
                         v)]]]))}]])
