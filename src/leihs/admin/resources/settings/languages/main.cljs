(ns leihs.admin.resources.settings.languages.main
  (:refer-clojure :exclude [str keyword])
  (:require
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [<! go timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.icons :as admin.common.icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.settings.icons :as icons]
    [leihs.admin.resources.settings.languages.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]))


(defonce data* (reagent/atom nil))

(defonce edit?* (reagent/atom false))

(defn fetch [& _]
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request
                      :chan <! http-client/filter-success :body))))

(defn put [& _]
  (go (when-let [data (some->
                         {:chan (async/chan)
                          :json-params @data*
                          :method :put}
                         http-client/request
                         :chan <!
                         http-client/filter-success :body)]
         (reset! data* data)
         (reset! edit?* false))))

(defn form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (put))}
   [:table.table.table-striped.table-sm
    [:tbody
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
            :disabled (or (not @edit?*)
                          (get-in @data* [locale :default]))]]
          [:td.default
           [form-components/checkbox-component data*
            [locale :default]
            :key (str locale "-" :default)
            :disabled (or (not @edit?*)
                          (-> @data* (get-in [locale :active]) not)
                          (get-in @data* [locale :default]))
            :pre-change (fn [v]
                          (doseq [locale (keys @data*)]
                            (swap! data* assoc-in [locale :default] false))
                          v )]]]))]]

   (when @edit?*
     [form-components/save-submit-component])])

(defn main-component []
  (if-not @data*
    [wait-component]
    [form-component]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @data*))]]))

(defn page []
  [:div.settings-page
   [routing/hidden-state-component
    {:did-mount (fn [& _]
                  (reset! edit?* false)
                  (fetch))}]
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[:li.breadcrumb-item
      [:button.btn
       {:class (if @edit?*
                 core-breadcrumbs/disabled-button-classes
                 core-breadcrumbs/enabled-button-classes)
        :on-click #(reset! edit?* true)}
       [admin.common.icons/edit] " Edit"]]]]
   [:h1 icons/languages " Languages Settings"]
   [main-component]
   [debug-component]
   ])
