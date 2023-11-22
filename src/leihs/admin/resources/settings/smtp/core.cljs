(ns leihs.admin.resources.settings.smtp.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [react-bootstrap :as react-bootstrap :refer [Col Form Row]]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request :chan <!
                      http-client/filter-success :body))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn form [action]
  [:> Form
   {:id "smtp-form"
    :on-submit (fn [e]
                 (.preventDefault e)
                 (action))}
   [:div.my-3
    [form-components/checkbox-component data* [:enabled]
     :label "Sending emails enabled"]]
   [:> Row
    [:> Col
     [form-components/input-component data* [:port]
      :type :number :label "Server port"]]
    [:> Col
     [form-components/input-component data* [:address]
      :label "Server address"]]]
   [:> Row
    [:> Col
     [form-components/input-component data* [:domain]
      :label "Domain name"]]
    [:> Col
     [form-components/input-component data* [:default_from_address]
      :label "From"]]
    [:> Col
     [form-components/input-component data* [:sender_address]
      :label "Sender"]]]

   [:> Row
    [:> Col
     [form-components/input-component data* [:username]
      :label "User"]]
    [:> Col
     [form-components/input-component data* [:password]
      :label "Password"]]]
   [:> Row
    [:> Col
     [form-components/input-component data* [:authentication_type]]]
    [:> Col
     [form-components/input-component data* [:openssl_verify_mode]]]
    [:> Col
     [form-components/checkbox-component data* [:enable_starttls_auto]]]]])
