(ns leihs.admin.resources.settings.syssec.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.syssec.core :as syssec-core]
   [leihs.admin.resources.settings.syssec.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @syssec-core/data*))]]))

(defn info-table []
  (let [data @syssec-core/data*]
    (fn []
      [table/container
       {:borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.external-base-url
          [:td "External Base URL" [:small " (external_base_url)"]]
          [:td (:external_base_url data)]]
         [:tr.instance-element
          [:td "Instance Element" [:small " (instance_element)"]]
          [:td (:instance_element data)]]
         [:tr.sessions-max-lifetime-secs
          [:td "Sessions Max Lifetime Secs" [:small " (sessions_max_lifetime_secs)"]]
          [:td (str (:sessions_max_lifetime_secs data))]]
         [:tr.sessions-force-secure
          [:td "Sessions Force Secure" [:small " (sessions_force_secure)"]]
          [:td (str (:sessions_force_secure data))]]
         [:tr.sessions-force-uniqueness
          [:td "Sessions Force Uniqueness" [:small " (sessions_force_uniqueness)"]]
          [:td (str (:sessions_force_uniqueness data))]]
         [:tr.public-image-caching-enabled
          [:td "Public Image Caching Enabled" [:small " (public_image_caching_enabled)"]]
          [:td (str (:public_image_caching_enabled data))]]]}])))

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
    {:did-change syssec-core/clean-and-fetch}]
   (if-not @syssec-core/data*
     [wait-component]
     [:article.settings-page.smtp
      [:header.my-5
       [:h1 [icons/shield-halved] " System and Security Settings"]]
      [:section
       [info-table]
       [edit-button]
       [debug-component]]])])
