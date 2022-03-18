(ns leihs.admin.resources.settings.misc.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [reagent.ratom :as ratom :refer [reaction]]
   [cljs.core.async.macros :refer [go]])
  (:require
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.breadcrumbs :as core-breadcrumbs]
   [leihs.admin.common.icons :as admin.common.icons]

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
      :disabled (not @edit?*)
      :hint [:span
             "Can be just a filename which is located under "
             [:code "/leihs/legacy/public/assets"]
             " or an absolute URL. "
             "The logo is displayed in the topbar of the legacy application."]]]
    [:div.col-sm
     [form-components/input-component data* [:documentation_link]
      :disabled (not @edit?*)
      :hint [:span "Absolute URL for a documentation resource if available. "
             "It is displayed under " [:cite "leihs"]
             " in the footer of the legacy application."]]]]

   [:div.row
    [:div.col-md
     [form-components/input-component data* [:contract_lending_party_string]
      :disabled (not @edit?*)
      :hint [:span "Displayed in contracts in addition to the respective inventory pool name."]]]
    [:div.col-md
     [form-components/input-component data* [:custom_head_tag]
      :disabled (not @edit?*) :rows 3 :element :textarea
      :hint [:span "Custom html tag to be rendered in the layout of the legacy application."]]]]

   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:time_zone]
      :disabled (not @edit?*)
      :label "Location"
      :hint [:span
             "The corresponding time zone is determined using "
             [:a {:href "https://en.wikipedia.org/wiki/Tz_database"} "tz database"]
             ". It is used for the configuration of the legacy application."]]]
    [:div.col-sm
     [form-components/input-component data* [:local_currency_string]
      :disabled (not @edit?*)
      :hint [:span
             "The international 3-letter code as defined by the "
             [:a {:href "https://de.wikipedia.org/wiki/ISO_4217"} "ISO 4217 standard"]
             "."]]]]


   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:maximum_reservation_time]
      :disabled (not @edit?*) :type :number
      :hint [:span "Maximum duration of reservations in days which applies to all inventory pools."]]]
    [:div.col-sm
     [form-components/input-component data* [:timeout_minutes]
      :disabled (not @edit?*) :type :number
      :hint [:span "Timeout of the borrow reservation cart in minutes."]]]]

   [:div.row
    [:div.col-sm-4
     [form-components/checkbox-component data* [:deliver_received_order_notifications]
      :disabled (not @edit?*)]]
    [:div.col-sm
     [form-components/input-component data* [:email_signature]
      :disabled (not @edit?*) :rows 3 :element :textarea]]]

   [:div.row
    [:div.col-sm-4.mb-2
     [form-components/checkbox-component data* [:include_customer_email_in_contracts]
      :disabled (not @edit?*)
      :hint [:span "If enabled, the contact email address of the lender will be included in the contract documents."]]]]

   [:div.row
    [:div.col-sm-4
     [form-components/checkbox-component data* [:lending_terms_acceptance_required_for_order]
      :disabled (not @edit?*)
      :hint [:span "Option to activate the obligation to accept the lending terms before submiting an order."]]]
    [:div.col-sm
     [form-components/input-component data* [:lending_terms_url]
      :disabled (not @edit?*)
      :hint [:span "Absolute URL for the web resource containing the lending terms. Required if "
             [:code "lending_terms_acceptance_required_for_order"]
             " is checked."]]]]

    [:div.row
     [:div.col-sm-4.mb-2
      [form-components/checkbox-component data* [:show_contact_details_on_customer_order]
       :disabled (not @edit?*)
       :hint [:span "If enabled, the contact details field will be shown on the customer order before submitting."]]]]

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
   [:h1 icons/misc " Miscellaneous Settings"]
   [main-component]
   [debug-component]])
