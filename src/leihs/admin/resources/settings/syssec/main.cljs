(ns leihs.admin.resources.settings.syssec.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.syssec.core :as core]
   [leihs.admin.resources.settings.syssec.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @core/data*))]]))

(defn info-table []
  [table/container
   {:borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.external-base-url
      [:td "External Base URL" [:small " (external_base_url)"]]
      [:td (:external_base_url @core/data*)]]
     [:tr.instance-element
      [:td "Instance Element" [:small " (instance_element)"]]
      [:td (:instance_element @core/data*)]]
     [:tr.sessions-max-lifetime-secs
      [:td "Sessions Max Lifetime Secs" [:small " (sessions_max_lifetime_secs)"]]
      [:td (str (:sessions_max_lifetime_secs @core/data*))]]
     [:tr.sessions-force-secure
      [:td "Sessions Force Secure" [:small " (sessions_force_secure)"]]
      [:td (str (:sessions_force_secure @core/data*))]]
     [:tr.sessions-force-uniqueness
      [:td "Sessions Force Uniqueness" [:small " (sessions_force_uniqueness)"]]
      [:td (str (:sessions_force_uniqueness @core/data*))]]
     [:tr.public-image-caching-enabled
      [:td "Public Image Caching Enabled" [:small " (public_image_caching_enabled)"]]
      [:td (str (:public_image_caching_enabled @core/data*))]]]}])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [wait-component]
     [:article.settings-page.smtp

      [:header.my-5
       [:h1 [icons/shield-halved] " System and Security Settings"]]

      [:section.mb-5
       [info-table]
       [edit/button]
       [edit/dialog]]

      [debug-component]])])
