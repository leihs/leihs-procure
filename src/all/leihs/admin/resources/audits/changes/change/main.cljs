(ns leihs.admin.resources.audits.changes.change.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.audits.changes.change.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.audits.changes.shared :refer [default-query-params]]
    [leihs.admin.resources.audits.core :as audits]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.utils.clipboard :as clipboard]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))


;;; changes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce change* (reagent/atom nil))

(defonce audited-change-id*
  (reaction (or (-> @routing/state* :route-params :audited-change-id)
                ":audited-change-id")))



(defn dt-change-content-component [data]
  [:dt.col-sm-5
   (let [text (if (string? data) data (with-out-str (pprint data)))]
     [:<>
      [:div {:style {:position :absolute :right "1em" :bottom 0}}
           [clipboard/button-tiny text]]
      [:pre text]])])

(defn head-change-component [data]
  [:h3 "Change " [:span.text-monospace (:tg_op data)]
   " on table " [:span.text-monospace (:table_name data)]
   " at " [:span.text-monospace (:created_at data)]
   " for primary key "
   [components/truncated-id-component (:pkey data) :max-length 8] "."])

(defn change-component [data]
  [:div.change
   [head-change-component data]
   [:ol.list-unstyled.list-group
    [:li.list-group-item.list-group-item-dark.p-2
     [:dl.row.my-1
      [:dt.col-sm-2 [:strong "Attribute"]]
      [:dt.col-sm-5 [:strong "Before"]]
      [:dt.col-sm-5 [:strong "After"]]]]
    (for [[i [k [before after]]] (map-indexed (fn [i v] [i v]) (:changed data))]
      ^{:key k} [:li.list-group-item.p-2
                 {:class (if (odd? i) "list-group-item-secondary" "")}
                 [:dl.row.my-1
                  [:dt.col-sm-2 k]
                  [dt-change-content-component before]
                  [dt-change-content-component after]]])]])

(defn main-component [id]
  [http/request-response-component
   {:url (path :audited-change {:audited-change-id id})}
   change-component ])

(defn page []
  [:div.audited-change
   [breadcrumbs/nav-component
    @breadcrumbs/left* []]
   [:h1 audits/icon-change " Audited-Change " ]
   [main-component @audited-change-id*]])
