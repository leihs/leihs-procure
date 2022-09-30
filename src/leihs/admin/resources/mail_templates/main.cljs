(ns leihs.admin.resources.mail-templates.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.mail-templates.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.mail-templates.shared :as shared]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.auth.core :as auth :refer []]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defonce languages-data* (reagent/atom nil))

(defn fetch-mail-templates []
  (http/route-cached-fetch data*))

(defn fetch-languages []
  (go (reset! languages-data*
              (some->
                {:chan (async/chan)
                 :url (path :languages-settings)}
                http/request :chan <!
                http/filter-success!
                :body))))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn link-to-mail-template
  [mail-template inner & {:keys [authorizers]
                     :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :mail-template {:mail-template-id (:id mail-template)})} inner]
    inner))

(defn language-locales-options [data]
  (->> @languages-data* vals
       (map :locale)
       (cons [nil "(any value)"])))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/form-term-filter-component]
     [routing/select-component
      :label "Name"
      :query-params-key :name
      :options [[nil "(any value)"]
                "approved"
                "deadline_soon_reminder"
                "received"
                "rejected"
                "reminder"
                "submitted"]]
     [routing/select-component
      :label "Type"
      :query-params-key :type
      :options {nil "(any value)"
                "order" "order"
                "user" "user"}]
     [routing/select-component
      :label "Language-Locale"
      :query-params-key :language_locale
      :options (language-locales-options @data*)]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [mail-template]
  [:td {:key :name}
   [link-to-mail-template mail-template [:span (:name mail-template)]
    :authorizers [auth/admin-scopes?]]])

(defn type-th-component []
  [:th.text-left {:key :type} "Type"])

(defn type-td-component [mail-template]
  [:td.text-left {:key :type} (:type mail-template)])

(defn language-locale-th-component []
  [:th.text-left {:key :language_locale} "Language-Locale"])

(defn language-locale-td-component [mail-template]
  [:td.text-left {:key :language_locale} (:language_locale mail-template)])

;;;;;

(defn mail-templates-thead-component [more-cols]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    (for [[idx col] (map-indexed vector more-cols)]
      ^{:key idx} [col])]])

(defn mail-template-row-component [mail-template more-cols]
  ^{:key (:id mail-template)}
  [:tr.mail-template {:key (:id mail-template)}
   [:td {:key :index} (:index mail-template)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col mail-template])])

(defn core-table-component [hds tds mail-templates]
  (if-let [mail-templates (seq mail-templates)]
    [:table.mail-templates.table.table-striped.table-sm
     [mail-templates-thead-component hds]
     [:tbody
      (doall (for [mail-template mail-templates]
               ^{:key (:id mail-template)}
               [mail-template-row-component mail-template tds]))]]
    [:div.alert.alert-warning.text-center "No (more) mail-templates found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :mail-templates)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-paramerters-normalized*"]
      [:pre (with-out-str (pprint @current-query-paramerters-normalized*))]]
     [:div
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@languages-data*"]
      [:pre (with-out-str (pprint @languages-data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component {:did-change fetch-mail-templates
                                    :did-mount fetch-languages}]
   [:div.alert.alert-info {:role "alert"}
    (str "These are intial mail templates which are copied for a new inventory pool when it gets created. "
         "They can then be further edited inside a particular inventory pool.")]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [name-th-component type-th-component language-locale-th-component]
    [name-td-component type-td-component language-locale-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.mail-templates
   [breadcrumbs/nav-component @breadcrumbs/left*]
   [:h1 [icons/mail-template] " Mail-Templates"]
   [main-page-content-component]])
