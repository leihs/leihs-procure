(ns leihs.admin.resources.system.authentication-systems.authentication-system.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.system.authentication-systems.authentication-system.core :as auth-core]
   [leihs.admin.resources.system.authentication-systems.authentication-system.delete :as delete]
   [leihs.admin.resources.system.authentication-systems.authentication-system.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [leihs.core.url.shared]
   [react-bootstrap :as react-bootstrap :refer [Button]]
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
      [:pre (with-out-str (pprint @auth-core/data*))]]]))

(defn info-table []
  (let [data @auth-core/data*]
    (fn []
      [table/container
       {:borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "ID" [:small " (id)"]]
          [:td  (:id data)]]
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td (:name data)]]
         [:tr.type
          [:td "Type" [:small " (type)"]]
          [:td (:type data)]]
         [:tr.sign_up_email_match
          [:td "Sign Up email address match" [:small " (sign_up_email_match)"]]
          [:td (:sign_up_email_match data)]]
         [:tr.priority
          [:td "Priority" [:small " (priority)"]]
          [:td (:priority data)]]
         [:tr.enabled
          [:td "Enabled" [:small " (enabled)"]]
          [:td (str (:enabled data))]]
         [:tr.send_email
          [:td "Send Mail" [:small " (send_email)"]]
          [:td (str (:send_email data))]]
         [:tr.send_org_id
          [:td "Send Org ID" [:small " (send_org_id)"]]
          [:td (str (:send_org_id data))]]
         [:tr.send_login
          [:td "Send Login" [:small " (send_login)"]]
          [:td (str (:send_login data))]]
         [:tr.description
          [:td "Description" [:small " (description)"]]
          [:td {:style {:white-space :break-spaces}}
           (str (:description data))]]
         [:tr.external_sign_in_url
          [:td "External Sign In URL" [:small " (external_sign_in_url)"]]
          [:td (str (:external_sign_in_url data))]]
         [:tr.external_sign_out_url
          [:td "External Sign out URL" [:small " (external_sign_out_url)"]]
          [:td (str (:external_sign_out_url data))]]
         [:tr.internal_private_key
          [:td "Internal Private Key" [:small " (internal_private_key)"]]
          [:td {:style {:white-space :break-spaces :filter "blur(7px)"}}
           (str (:internal_private_key data))]]
         [:tr.internal_public_key
          [:td "Internal Public Key" [:small " (internal_public_key)"]]
          [:td {:style {:white-space :break-spaces}}
           (str (:internal_public_key data))]]
         [:tr.external_public_key
          [:td "External Private Key" [:small " (external_public_key)"]]
          [:td {:style {:white-space :break-spaces}}
           (str (:external_public_key data))]]]}])))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))

(defn delete-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:variant "danger"
         :className "ml-3"
         :onClick #(reset! show true)}
        "Delete"]
       [delete/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change auth-core/clean-and-fetch}]
   (if-not @auth-core/data*
     [:div.my-5
      [wait-component " Loading Authentication System Data ..."]]
     [:article.authentication-system.my-5
      [auth-core/header]
      [:section
       [auth-core/tabs "info"]
       [info-table]
       [edit-button]
       [delete-button]
       [debug-component]]])])
