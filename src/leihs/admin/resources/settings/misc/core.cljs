(ns leihs.admin.resources.settings.misc.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.http-client.core :as http-client]
   [react-bootstrap :as react-bootstrap :refer [Col Form Row]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request :chan <!
                      http-client/filter-success :body))))

(defn form [action data*]
  [:> Form
   {:id "misc-form"
    :on-submit (fn [e]
                 (.preventDefault e)
                 (action))}

   [:> Row
    [:> Col
     [:> Form.Group {:id "logo_url"}
      [:> Form.Label "Logo URL"]
      [:input.form-control
       {:type "text"
        :id "logo_url"
        :value (or (:logo_url @data*) "")
        :onChange #(swap! data* assoc :logo_url (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Can be just a filename which is located under "
       [:code "/leihs/legacy/public/assets"]
       " or an absolute URL. "
       "The logo is displayed in the topbar of the legacy application."]]]

    [:> Col
     [:> Form.Group {:id "documentation_link"}
      [:> Form.Label "Documentation Link"]
      [:input.form-control
       {:type "text"
        :id "documentation_link"
        :value (or (:documentation_link @data*) "")
        :onChange #(swap! data* assoc :documentation_link (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Absolute URL for a documentation resource if available. "
       "It is displayed under " [:cite "leihs"]
       " in the footer of the legacy application."]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "contract_lending_party_string"}
      [:> Form.Label "Contract Lending Party String"]
      [:input.form-control
       {:type "text"
        :id "contract_lending_party_string"
        :value (or (:contract_lending_party_string @data*) "")
        :onChange #(swap! data* assoc :contract_lending_party_string (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Displayed in contracts in addition to the respective inventory pool name."]]]

    [:> Col
     [:> Form.Group {:id "custom_head_tag"}
      [:> Form.Label "Custom Head Tag"]
      [:input.form-control
       {:as "textarea"
        :id "custom_head_tag"
        :rows 3
        :value (or (:custom_head_tag @data*) "")
        :onChange #(swap! data* assoc :custom_head_tag (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Custom html tag to be rendered in the layout of the legacy application."]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "time_zone"}
      [:> Form.Label "Location"]
      [:input.form-control
       {:type "text"
        :id "time_zone"
        :value (or (:time_zone @data*) "")
        :onChange #(swap! data* assoc :time_zone (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "The corresponding time zone is determined using "
       [:a {:href "https://en.wikipedia.org/wiki/Tz_database"} "tz database"]
       ". It is used for the configuration of the legacy application."]]]

    [:> Col
     [:> Form.Group {:id "local_currency_string"}
      [:> Form.Label "Local Currency String"]
      [:input.form-control
       {:type "text"
        :id "local_currency_string"
        :value (or (:local_currency_string @data*) "")
        :onChange #(swap! data* assoc :local_currency_string (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "The international 3-letter code as defined by the "
       [:a {:href "https://de.wikipedia.org/wiki/ISO_4217"} "ISO 4217 standard"]
       "."]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "maximum_reservation_time"}
      [:> Form.Label "Maximum Reservation Time"]
      [:input.form-control
       {:type "number"
        :id "maximum_reservation_time"
        :value (or (:maximum_reservation_time @data*) "")
        :onChange #(swap! data* assoc :maximum_reservation_time (-> % .-target .-value int))}]
      [:> Form.Text {:className "text-muted"}
       "Maximum duration of reservations in days which applies to all inventory pools."]]]

    [:> Col
     [:> Form.Group {:id "timeout_minutes"}
      [:> Form.Label "Timeout Minutes"]
      [:input.form-control
       {:type "number"
        :id "timeout_minutes"
        :value (or (:timeout_minutes @data*) "")
        :onChange #(swap! data* assoc :timeout_minutes (-> % .-target .-value int))}]
      [:> Form.Text {:className "text-muted"}
       "Timeout of the borrow reservation cart in minutes."]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "deliver_received_order_notifications"}
      [:> Form.Check
       {:type "checkbox"
        :id "deliver_received_order_notifications"
        :label "Deliver Received Order Notifications"
        :checked (:deliver_received_order_notifications @data*)
        :onChange #(swap! data* assoc :deliver_received_order_notifications (-> % .-target .-checked))}]]]
    [:> Col
     [:> Form.Group {:id "email_signature"}
      [:> Form.Label "Email Signature"]
      [:textarea.form-control
       {:id "email_signature"
        :value (or (:email_signature @data*) "")
        :onChange #(swap! data* assoc :email_signature (-> % .-target .-value))}]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "include_customer_email_in_contracts"}
      [:> Form.Check
       {:type "checkbox"
        :id "include_customer_email_in_contracts"
        :label "Include Customer Email in Contracts"
        :checked (:include_customer_email_in_contracts @data*)
        :onChange #(swap! data* assoc :include_customer_email_in_contracts (-> % .-target .-checked))}]
      [:> Form.Text {:className "text-muted"}
       "If enabled, the contact email address of the lender will be included in the contract documents."]]]]

   [:> Row
    [:> Col {:sm 4}
     [:> Form.Group {:id "lending_terms_acceptance_required_for_order"}
      [:> Form.Check
       {:type "checkbox"
        :id "lending_terms_acceptance_required_for_order"
        :label "Lending Terms Acceptance Required for Order"
        :checked (:lending_terms_acceptance_required_for_order @data*)
        :onChange #(swap! data* assoc :lending_terms_acceptance_required_for_order (-> % .-target .-checked))}]]]
    [:> Col
     [:> Form.Group {:id "lending_terms_url"}
      [:> Form.Label "Lending Terms URL"]
      [:input.form-control
       {:type "text"
        :id "lending_terms_url"
        :value (or (:lending_terms_url @data*) "")
        :onChange #(swap! data* assoc :lending_terms_url (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Absolute URL for the web resource containing the lending terms. Required if "
       [:code "lending_terms_acceptance_required_for_order"]
       " is checked."]]]]

   [:> Row
    [:> Col {:sm 4 :className "mb-2"}
     [:> Form.Group {:id "show_contact_details_on_customer_order"}
      [:> Form.Check
       {:type "checkbox"
        :id "show_contact_details_on_customer_order"
        :label "Show Contact Details on Customer Order"
        :checked (:show_contact_details_on_customer_order @data*)
        :onChange #(swap! data* assoc :show_contact_details_on_customer_order (-> % .-target .-checked))}]
      [:> Form.Text {:className "text-muted"}
       "If enabled, the contact details field will be shown on the customer order before submitting."]]]]

   [:> Row
    [:> Col
     [:> Form.Group {:id "home_page_image_url"}
      [:> Form.Label "Homepage Image"]
      [:input.form-control
       {:type "text"
        :id "home_page_image_url"
        :value (or (:home_page_image_url @data*) "")
        :onChange #(swap! data* assoc :home_page_image_url (-> % .-target .-value))}]
      [:> Form.Text {:className "text-muted"}
       "Absolute URL of the image to display on the home page (max 2000 characters). If left empty then the default image is used."]]]]])
