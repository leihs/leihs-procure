(ns leihs.admin.resources.settings.misc.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.misc.core :as core]
   [leihs.admin.resources.settings.misc.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn info-table []
  [table/container
   {:borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.logo-url
      [:td "Logo URL" [:small " (logo_url)"]]
      [:td.logo-url (:logo_url @core/data*)]]
     [:tr.documentation-link
      [:td "Documentation Link" [:small " (documentation_link)"]]
      [:td.documentation-link (:documentation_link @core/data*)]]
     [:tr.contract-lending-party-string
      [:td "Contract Lending Party String" [:small " (contract_lending_party_string)"]]
      [:td.contract-lending-party-string (:contract_lending_party_string @core/data*)]]
     [:tr.custom_head_tag
      [:td "Custom Head Tag" [:small " (custom_head_tag)"]]
      [:td.custom_head_tag (:custom_head_tag @core/data*)]]
     [:tr.time-zone
      [:td "Location" [:small " (time_zone)"]]
      [:td.time-zone (:time_zone @core/data*)]]
     [:tr.local-currency-string
      [:td "Location" [:small " (local_currency_string)"]]
      [:td.local-currency-string (:local_currency_string @core/data*)]]
     [:tr.maximum-reservation-time
      [:td "Maximum Reservation Time" [:small " (maximum_reservation_time)"]]
      [:td.maximum-reservation-time (:maximum_reservation_time @core/data*)]]
     [:tr.timeout-minutes
      [:td "Timeout (minutes)" [:small " (timeout_minutes)"]]
      [:td.timeout-minutes (:timeout_minutes @core/data*)]]
     [:tr.deliver-received-order-notifications
      [:td "Deliver Received Order Notifications" [:small " (deliver_received_order_notifications)"]]
      [:td.deliver-received-order-notifications (str (:deliver_received_order_notifications @core/data*))]]
     [:tr.email-signature
      [:td "Email Signature" [:small " (email_signature)"]]
      [:td.email-signature {:style {:white-pace "break-spaces"}}
       (str (:email_signature @core/data*))]]
     [:tr.include-customer-email-in-contracts
      [:td "Include Customer Email in Contracts" [:small " (include_customer_email_in_contracts)"]]
      [:td.include-customer-email-in-contracts (str (:include_customer_email_in_contracts @core/data*))]]
     [:tr.lending-terms-acceptance-required-for_order
      [:td "Lending Terms Acceptance required for Order" [:small " (lending_terms_acceptance_required_for_order)"]]
      [:td.lending-terms-acceptance-required-for_order (str (:lending_terms_acceptance_required_for_order @core/data*))]]
     [:tr.lending-terms-url
      [:td "Lending Terms URL" [:small " (lending_terms_url)"]]
      [:td.lending-terms-url (str (:lending_terms_url @core/data*))]]
     [:tr.show-contact-details-on-customer-order
      [:td "Show Contact Details on Customer Order" [:small " (show_contact_details_on_customer_order)"]]
      [:td.show-contact-details-on-customer-order (str (:show_contact_details_on_customer_order @core/data*))]]
     [:tr.home-page-image-url
      [:td "Home Page Image URL" [:small " (home_page_image_url)"]]
      [:td (str (:home_page_image_url @core/data*))]]]}])

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@misc-core/data*"]
     [:pre (with-out-str (pprint @core/data*))]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount core/fetch}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]
     [:article.settings-page
      [:header.my-5
       [:h1 [icons/list-icon] " Miscellaneous Settings"]]

      [:section.mb-5
       [info-table]
       [edit/button]
       [edit/dialog]]

      [debug-component]])])
