(ns leihs.admin.resources.settings.smtp.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.smtp.core :as core]
   [leihs.admin.resources.settings.smtp.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn info-table []
  [table/container
   {:borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.enabled
      [:td "Sending EMails enabled" [:small " (enabled)"]]
      [:td (str (:enabled @core/data*))]]
     [:tr.port
      [:td "Server Port" [:small " (port)"]]
      [:td (:port @core/data*)]]
     [:tr.address
      [:td "Server Address" [:small " (address)"]]
      [:td (:address @core/data*)]]
     [:tr.domain
      [:td "Domain Name" [:small " (domain)"]]
      [:td (:domain @core/data*)]]
     [:tr.default-from-address
      [:td "From" [:small " (default_from_address)"]]
      [:td (:default_from_address @core/data*)]]
     [:tr.sender-address
      [:td "Sender Address" [:small " (sender_address)"]]
      [:td (:sender_address @core/data*)]]
     [:tr.username
      [:td "User Name" [:small " (username)"]]
      [:td (:username @core/data*)]]
     [:tr.password
      [:td "Password" [:small " (password)"]]
      [:td (:password @core/data*)]]
     [:tr.authentication-type
      [:td "Authentication Type" [:small " (authentication_type)"]]
      [:td (:authentication_type @core/data*)]]
     [:tr.openssl-verify-mode
      [:td "OpenSSL Verify Mode" [:small " (openssl_verify_mode)"]]
      [:td (:openssl_verify_mode @core/data*)]]
     [:tr.enable-starttls-auto
      [:td "Enable Starttls Auto" [:small " (enable_starttls_auto)"]]
      [:td (str (:enable_starttls_auto @core/data*))]]]}])

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @core/data*))]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]
     [:article.settings-page.smtp
      [:header.my-5
       [:h1 [icons/paper-plane] " SMTP Settings"]]

      [:section.mb-5
       [info-table]
       [edit/button]
       [edit/dialog]]

      [debug-component]])])
