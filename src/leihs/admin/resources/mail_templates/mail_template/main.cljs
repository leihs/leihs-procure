(ns leihs.admin.resources.mail-templates.mail-template.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.mail-templates.mail-template.core :as core]
   [leihs.admin.resources.mail-templates.mail-template.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn info-table []
  [table/container
   {:className "mail-template"
    :borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td.name  (:name @core/data*)]]
     [:tr.type
      [:td "Type" [:small " (type)"]]
      [:td.type (:type @core/data*)]]
     [:tr.language
      [:td "Language Locale" [:small " (language_locale)"]]
      [:td.language (:language_locale @core/data*)]]
     [:tr.format
      [:td "Format" [:small " (format)"]]
      [:td.format (:format @core/data*)]]
     [:tr.body {:style {:white-space "break-spaces"}}
      [:td "Body" [:small " (body)"]]
      [:td.body (:body @core/data*)]]
     [:tr.variables {:style {:white-space "break-spaces"}}
      [:td "Variables" [:small " (variables)"]]
      [:td.variables
       (core/template-variables)]]]}])

(defn header []
  (let [name (:name @core/data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main  {:to (path :mail-templates)}]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   (if-not @core/data*
     [:div.my-5
      [wait-component]]

     [:article.mail-template
      [header]

      [:section.mb-5
       [info-table]
       [edit/button]
       [edit/dialog]]

      [core/debug-component]])])
