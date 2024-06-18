(ns leihs.admin.resources.system.authentication-systems.authentication-system.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as core]
   [leihs.admin.resources.system.authentication-systems.authentication-system.delete :as delete]
   [leihs.admin.resources.system.authentication-systems.authentication-system.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [leihs.core.url.shared]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :authentication-system-id)
                ":authentication-system-id")))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.authentication-system-debug
     [:hr]
     [:div.authentication-system-data
      [:h3 "@auth-core/data*"]
      [:pre (with-out-str (pprint @core/data*))]]]))

(defn info-table []
  [table/container
   {:borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "ID" [:small " (id)"]]
      [:td  (:id @core/data*)]]
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td (:name @core/data*)]]
     [:tr.type
      [:td "Type" [:small " (type)"]]
      [:td (:type @core/data*)]]
     [:tr.sign_up_email_match
      [:td "Sign Up email address match" [:small " (sign_up_email_match)"]]
      [:td (:sign_up_email_match @core/data*)]]
     [:tr.priority
      [:td "Priority" [:small " (priority)"]]
      [:td (:priority @core/data*)]]
     [:tr.enabled
      [:td "Enabled" [:small " (enabled)"]]
      [:td (str (:enabled @core/data*))]]
     [:tr.send_email
      [:td "Send Mail" [:small " (send_email)"]]
      [:td (str (:send_email @core/data*))]]
     [:tr.send_org_id
      [:td "Send Org ID" [:small " (send_org_id)"]]
      [:td (str (:send_org_id @core/data*))]]
     [:tr.send_login
      [:td "Send Login" [:small " (send_login)"]]
      [:td (str (:send_login @core/data*))]]
     [:tr.description
      [:td "Description" [:small " (description)"]]
      [:td {:style {:white-space :break-spaces}}
       (str (:description @core/data*))]]
     [:tr.external_sign_in_url
      [:td "External Sign In URL" [:small " (external_sign_in_url)"]]
      [:td (str (:external_sign_in_url @core/data*))]]
     [:tr.external_sign_out_url
      [:td "External Sign out URL" [:small " (external_sign_out_url)"]]
      [:td (str (:external_sign_out_url @core/data*))]]
     [:tr.internal_private_key
      [:td "Internal Private Key" [:small " (internal_private_key)"]]
      [:td {:style {:white-space :break-spaces :filter "blur(7px)"}}
       (str (:internal_private_key @core/data*))]]
     [:tr.internal_public_key
      [:td "Internal Public Key" [:small " (internal_public_key)"]]
      [:td {:style {:white-space :break-spaces}}
       (str (:internal_public_key @core/data*))]]
     [:tr.external_public_key
      [:td "External Private Key" [:small " (external_public_key)"]]
      [:td {:style {:white-space :break-spaces}}
       (str (:external_public_key @core/data*))]]]}])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]

     [:article.authentication-system.my-5
      [core/header]

      [:section.mb-5
       [core/tabs "info"]
       [info-table]
       [edit/button]
       [edit/dialog]
       [delete/button]
       [delete/dialog]]

      [debug-component]])])
