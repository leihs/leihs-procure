(ns leihs.admin.resources.settings.smtp.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.admin.common.icons :as admin.common.icons]

    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.settings.icons :as icons]
    [leihs.admin.resources.settings.smtp.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


(defonce data* (reagent/atom nil))

(defonce edit?* (reagent/atom false))

(defn fetch [& _]
  (go (reset! data*
              (some-> {:chan (async/chan)}
                      http-client/request
                      :chan <! http-client/filter-success :body))))

(defn put [& _]
  (go (when-let [data (some->
                         {:chan (async/chan)
                          :json-params @data*
                          :method :put}
                         http-client/request
                         :chan <!
                         http-client/filter-success :body)]
         (reset! data* data)
         (reset! edit?* false))))

(defn form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (put))}
   [:div.my-3
    [form-components/checkbox-component data* [:enabled]
     :disabled (not @edit?*) :label "Sending emails enabled"]]
   [:div.row
    [:div.col-sm-4
     [form-components/input-component data* [:port]
      :disabled (not @edit?*) :type :number :label "Server port"]]
    [:div.col-sm-8
     [form-components/input-component data* [:address]
      :disabled (not @edit?*) :label "Server address"]]]
   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:domain]
      :disabled (not @edit?*) :label "Domain name"]]
    [:div.col-sm
     [form-components/input-component data* [:default_from_address]
      :disabled (not @edit?*) :label "From"]]
    [:div.col-sm
     [form-components/input-component data* [:sender_address]
      :disabled (not @edit?*) :label "Sender"]]]
   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:username]
      :disabled (not @edit?*) :label "User"]]
    [:div.col-sm
     [form-components/input-component data* [:password]
      :disabled (not @edit?*) :label "Password"]]]
   [:div.row
    [:div.col-sm
     [form-components/input-component data* [:authentication_type]
      :disabled (not @edit?*)]]
    [:div.col-sm
     [form-components/input-component data* [:openssl_verify_mode]
      :disabled (not @edit?*)]]
    [:div.col-sm
     [form-components/checkbox-component data* [:enable_starttls_auto]
      :disabled (not @edit?*)]]]
   (when @edit?*
     [form-components/save-submit-component])])

(defn main-component []
  (if-not @data*
    [wait-component]
    [form-component]))

(defn debug-component []
  (when @state/debug?*
    [:div.debug
     [:h3 "@data*"]
     [:pre (with-out-str (pprint @data*))]]))

(defn page []
  [:div.settings-page
   [routing/hidden-state-component
    {:did-mount (fn [& _]
                  (reset! edit?* false)
                  (fetch))}]
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[:li.breadcrumb-item
      [:button.btn
       {:class (if @edit?*
                 core-breadcrumbs/disabled-button-classes
                 core-breadcrumbs/enabled-button-classes)
        :on-click #(reset! edit?* true)}
       [admin.common.icons/edit] " Edit"]]]]
   [:h1 icons/smtp " SMTP Settings"]
   [main-component]
   [debug-component]
   ])
