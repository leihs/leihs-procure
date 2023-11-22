(ns leihs.admin.resources.settings.languages.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.languages.core :as languages-core]
   [leihs.admin.resources.settings.languages.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Form]]
   [reagent.core :as reagent]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @languages-core/data*))]]))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))
(defn info-table []
  (let [data @languages-core/data*]
    (fn []
      [table/container
       {:header [:tr
                 [:th "Locale"]
                 [:th "Name"]
                 [:th "Active"]
                 [:th "Default"]]
        :body
        (doall
         (for [[locale lang] data]
           ^{:key locale}
           [:tr
            [:td (:locale lang)]
            [:td [:span (:name lang)]]
            [:td [:> Form.Check {:id (str (:locale lang) "-active")
                                 :disabled true
                                 :type "switch"
                                 :checked (:active lang)}]]
            [:td [:> Form.Check {:id (str (:locale lang) "-default")
                                 :disabled true
                                 :type "switch"
                                 :checked (:default lang)}]]]))}])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change languages-core/clean-and-fetch}]
   (if-not @languages-core/data*
     [wait-component]
     [:article.settings-page.smtp
      [:header.my-5
       [:h1 [icons/language] " Languages Settings"]]
      [:section
       [info-table]
       [edit-button]
       [debug-component]]])])

