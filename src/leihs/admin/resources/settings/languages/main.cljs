(ns leihs.admin.resources.settings.languages.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :refer [toggle-component]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.settings.languages.core :as core]
   [leihs.admin.resources.settings.languages.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @core/data*))]]))

(defn info-table []
  [table/container
   {:header [:tr
             [:th "Locale"]
             [:th "Name"]
             [:th "Active"]
             [:th "Default"]]
    :body
    (doall
     (for [[locale lang] @core/data*]
       ^{:key locale}
       [:tr
        [:td (:locale lang)]
        [:td [:span (:name lang)]]

        [:td.active
         [toggle-component (:active lang)]]

        [:td.default
         [toggle-component (:default lang)]]]))}])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [wait-component]
     [:article.settings-page.smtp
      [:header.my-5
       [:h1 [icons/language] " Languages Settings"]]

      [:section:mb-5
       [info-table]
       [edit/button]
       [edit/dialog]]

      [debug-component]])])
