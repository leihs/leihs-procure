(ns leihs.admin.resources.mail-templates.mail-template.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [cljs.core.async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.contrib.inflect :refer [pluralize-noun]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.mail-templates.breadcrumbs :as breadcrumbs-parent]
   [leihs.admin.resources.mail-templates.mail-template.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.mail-templates.mail-template.core :as mail-template
    :refer [clean-and-fetch id* data*]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(def template-variables-for-order-component
  [:ul
   [:li [:code :comment]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :order_url]]
   [:li [:code :purpose]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]]],
   [:li [:code :user.name]]])

(def template-variables-for-user-component
  [:ul
   [:li [:code :due_date]],
   [:li [:code :email_signature]],
   [:li [:code :inventory_pool.name]],
   [:li [:code :inventory_pool.description]],
   [:li [:code :quantity]],
   [:li [:code :reservations]
    [:ul
     [:li [:code :l.end_date]]
     [:li [:code :l.item_inventory_code]]
     [:li [:code :l.model_name]],
     [:li [:code :l.quantity]],
     [:li [:code :l.start_date]]]],
   [:li [:code :user.name]]])

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn data-li-dl-component [label param data]
  ^{:key param}
  [:li {:key param}
   [:dl.row.mb-0
    [:dt.col-6 {:style {:max-width "300px"}}
     [:span label [:small " (" [:span.text-monospace param] ")"]]]
    [:dd.col-6 (param data)]]])

(defn mail-template-component []
  [:div.mail-template.mt-3
   [:ul.list-unstyled
    [data-li-dl-component "Name" :name @mail-template/data*]
    [data-li-dl-component "Type" :type @mail-template/data*]
    [data-li-dl-component "Language-Locale" :language_locale @mail-template/data*]
    [data-li-dl-component "Format" :format @mail-template/data*]]
   [:div.row
    [:div.col-md
     [form-components/input-component mail-template/data* [:body]
      :element :textarea
      :label "Body"
      :rows 30
      :disabled (not @mail-template/edit-mode?*)]]
    [:div.col-md [:strong "Template-Variables"]
     (case (:type @mail-template/data* "order")
       "order" template-variables-for-order-component
       "user" template-variables-for-user-component
       nil)]]])

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [route (path :mail-template {:mail-template-id @mail-template/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @mail-template/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn edit-page []
  [:div.edit-mail-template
   [routing/hidden-state-component
    {:did-mount mail-template/clean-and-fetch}]
   (breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/edit-li]) [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Mail-Template "]
      [mail-template/name-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [mail-template-component]
    [form-components/save-submit-component]]
   [mail-template/debug-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.mail-template
   [routing/hidden-state-component {:did-mount #(clean-and-fetch)}]
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/mail-templates-li] [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Mail-Template "]
      [mail-template/name-link-component]]]]
   [mail-template-component]
   [mail-template/debug-component]])
