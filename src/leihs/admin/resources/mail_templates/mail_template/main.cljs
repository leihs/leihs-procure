(ns leihs.admin.resources.mail-templates.mail-template.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.mail-templates.mail-template.core :as mail-template-core]
   [leihs.admin.resources.mail-templates.mail-template.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button]]
   [reagent.core :as reagent]))

(defn info-table []
  (let [data @mail-template-core/data*]
    (fn []
      [table/container
       {:className "mail-template"
        :borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td.name  (:name data)]]
         [:tr.type
          [:td "Type" [:small " (type)"]]
          [:td.type (:type data)]]
         [:tr.language
          [:td "Language Locale" [:small " (language_locale)"]]
          [:td.language (:language_locale data)]]
         [:tr.format
          [:td "Format" [:small " (format)"]]
          [:td.format (:format data)]]
         [:tr.body {:style {:white-space "break-spaces"}}
          [:td "Body" [:small " (body)"]]
          [:td.body (:body data)]]
         [:tr.variables {:style {:white-space "break-spaces"}}
          [:td "Variables" [:small " (variables)"]]
          [:td.variables
           (mail-template-core/template-variables)]]]}])))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(reset! show true)}
        "Edit"]
       [edit/dialog {:show @show
                     :onHide #(reset! show false)}]])))

(defn header []
  (let [name (:name @mail-template-core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :mail-templates)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change mail-template-core/clean-and-fetch}]
   (if-not @mail-template-core/data*
     [:div.my-5
      [wait-component " Loading Mail Template Data ..."]]
     [:article.mail-template
      [header]
      [:section.mb-5
       [info-table]
       [edit-button]
       [mail-template-core/debug-component]]])])
