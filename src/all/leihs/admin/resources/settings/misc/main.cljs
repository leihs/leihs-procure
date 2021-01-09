(ns leihs.admin.resources.settings.misc.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.icons :as core-icons]

    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.settings.icons :as icons]
    [leihs.admin.resources.settings.misc.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
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

   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:logo_url]
      :disabled (not @edit?*)]]
    [:div.col-sm
     [form-components/input-component data* [:documentation_link]
      :disabled (not @edit?*)]]]

   [:div.row
    [:div.col-md
     [form-components/input-component data* [:contract_lending_party_string]
      :disabled (not @edit?*)]]
    [:div.col-md
     [form-components/input-component data* [:contract_terms]
      :disabled (not @edit?*) :rows 5 :element :textarea]]
    [:div.col-md
     [form-components/input-component data* [:custom_head_tag]
      :disabled (not @edit?*) :rows 3 :element :textarea]]]

   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:default_email]
      :disabled (not @edit?*)]]]

   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:time_zone]
      :disabled (not @edit?*)]]
    [:div.col-sm
     [form-components/input-component data* [:local_currency_string]
      :disabled (not @edit?*)]]]


   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:maximum_reservation_time]
      :disabled (not @edit?*) :type :number]]
    [:div.col-sm
     [form-components/input-component data* [:timeout_minutes]
      :disabled (not @edit?*) :type :number]]]

   [:div.row
    [:div.col-sm-3
     [form-components/checkbox-component data* [:disable_borrow_section]
      :disabled (not @edit?*)]]
    [:div.col-sm-9
     [form-components/input-component data* [:disable_borrow_section_message]
      :disabled (not @edit?*) :rows 3 :element :textarea]]]

   [:div.row
    [:div.col-sm-3
     [form-components/checkbox-component data* [:disable_manage_section]
      :disabled (not @edit?*)]]
    [:div.col-sm-9
     [form-components/input-component data* [:disable_manage_section_message]
      :disabled (not @edit?*) :rows 3 :element :textarea]]]


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
       core-icons/edit " Edit"]]]]
   [:h1 icons/misc " Miscellaneous Settings"]
   [main-component]
   [debug-component]
   ])
