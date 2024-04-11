(ns leihs.admin.resources.settings.smtp.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.smtp.core :as smtp-core]
   [leihs.admin.resources.settings.smtp.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn info-table []
  (let [data @smtp-core/data*]
    (fn []
      [table/container
       {:borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.enabled
          [:td "Sending EMails enabled" [:small " (enabled)"]]
          [:td (str (:enabled data))]]
         [:tr.port
          [:td "Server Port" [:small " (port)"]]
          [:td (:port data)]]
         [:tr.address
          [:td "Server Address" [:small " (address)"]]
          [:td (:address data)]]
         [:tr.domain
          [:td "Domain Name" [:small " (domain)"]]
          [:td (:domain data)]]
         [:tr.default-from-address
          [:td "From" [:small " (default_from_address)"]]
          [:td (:default_from_address data)]]
         [:tr.sender-address
          [:td "Sender Address" [:small " (sender_address)"]]
          [:td (:sender_address data)]]
         [:tr.username
          [:td "User Name" [:small " (username)"]]
          [:td (:username data)]]
         [:tr.password
          [:td "Password" [:small " (password)"]]
          [:td (:password data)]]
         [:tr.authentication-type
          [:td "Authentication Type" [:small " (authentication_type)"]]
          [:td (:authentication_type data)]]
         [:tr.openssl-verify-mode
          [:td "OpenSSL Verify Mode" [:small " (openssl_verify_mode)"]]
          [:td (:openssl_verify_mode data)]]
         [:tr.enable-starttls-auto
          [:td "Enable Starttls Auto" [:small " (enable_starttls_auto)"]]
          [:td (str (:enable_starttls_auto data))]]]}])))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @smtp-core/data*))]]))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change smtp-core/clean-and-fetch}]
   (if-not @smtp-core/data*
     [wait-component]
     [:article.settings-page.smtp
      [:header.my-5
       [:h1 [icons/paper-plane] " SMTP Settings"]]
      [:section
       [info-table]
       [edit-button]
       [debug-component]]])])
