(ns leihs.admin.resources.system.authentication-systems.authentication-system.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.core.routing.front :as routing]
   [leihs.core.url.shared]
   [react-bootstrap :as react-bootstrap :refer [Form Nav Alert Row Col]]
   [reagent.core :as reagent]
   [reagent.ratom :as ratom :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :authentication-system-id)
                ":authentication-system-id")))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :authentication-system
                           (-> @routing/state* :route-params))}
               http-client/request :chan <!
               http-client/filter-success! :body))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn form [action]
  [:> Form
   {:id "auth-form"
    :on-submit (fn [e]
                 (.preventDefault e)
                 (action))}
   [:h3 "Properties"]

   [:> Row
    [:> Col
     [form-components/input-component data* [:id]
      :label "Id"
      :required true
      :disabled (not= (-> @routing/state* :handler-key)
                      :authentication-systems)]]
    [:> Col
     [form-components/input-component data* [:name]
      :required true
      :label "Name"]]]

   [:> Row
    [:> Col {:xs 4}
     [form-components/input-component data* [:type]
      :required true
      :hint "external or password"
      :label "Type"]]
    [:> Col
     [form-components/input-component data* [:sign_up_email_match]
      :label "Sign-up email address match"]]]

   [:> Row
    [:> Col
     [form-components/input-component data* [:priority]
      :type :number
      :label "Priority"]]
    [:> Col
     [form-components/checkbox-component data* [:enabled]
      :label "Enabled"]]
    [:> Col
     [form-components/checkbox-component data* [:send_email]
      :label "Send email-address"]]
    [:> Col
     [form-components/checkbox-component data* [:send_org_id]
      :label "Send org_id"]]
    [:> Col
     [form-components/checkbox-component data* [:send_login]
      :label "Send login"]]]

   [:> Row
    [:> Col
     [form-components/input-component data* [:description]
      :element :textarea
      :rows 5
      :label "Description"]]]

   [:> Row
    [:> Col
     [form-components/input-component data* [:external_sign_in_url]
      :label "External sign-in URL"]]
    [:> Col
     [form-components/input-component data* [:external_sign_out_url]
      :label "External sign-out URL"]]]

   [form-components/input-component data* [:internal_private_key]
    :element :textarea
    :rows 5
    :label "Internal private key"]

   [form-components/input-component data* [:internal_public_key]
    :element :textarea
    :rows 5
    :label "Internal public key"]

   [:> Alert {:variant "secondary"}
    [:p "Leihs accepts " [:strong "ES256 keys only. "]
     "The openssl cli can be used to create a pair as in the following:"]
    [:div
     [:pre.code
      "openssl ecparam -name prime256v1 -genkey -noout -out tmp/key.pem\n"
      "openssl ec -in tmp/key.pem -pubout -out tmp/public.pem"]]]

   [form-components/input-component data* [:external_public_key]
    :element :textarea
    :rows 5
    :label "External public key"]])

(defn header []
  (let [name (:name @data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main {:href (path :authentication-systems {})}]
       [:h1.mt-3
        [:span " Authentication System - " name]]])))

(defn tabs [active]
  [:> Nav
   {:variant "tabs"
    :className "mt-5 mb-3"
    :defaultActiveKey "info"}
   [:> Nav.Item
    [:> Nav.Link
     {:active (when (= active "info") true)
      :href (path :authentication-system
                  {:authentication-system-id @id*})}
     "Info"]]
   [:> Nav.Item
    [:> Nav.Link
     {:active (when (= active "users") true)
      :href (path :authentication-system-user
                  {:authentication-system-id @id*
                   :user-id (:id @id*)})}
     "Users"]]
   [:> Nav.Item
    [:> Nav.Link
     {:active (when (= active "groups") true)
      :href (path :authentication-system-group
                  {:authentication-system-id @id*
                   :group-id (:id @id*)})}
     "Groups"]]])
