(ns leihs.admin.resources.rooms.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
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
   [leihs.admin.resources.rooms.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.rooms.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.seq :as seq]
   [leihs.core.auth.core :as auth :refer []]
   [leihs.core.core :refer [keyword str presence detect]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front :as current-user]
   [reagent.core :as reagent]))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defonce buildings-data* (reagent/atom nil))

(defn clean-and-fetch []
  (go (reset! buildings-data*
              (some->
               {:chan (async/chan)
                :url (path :buildings)}
               http/request :chan <!
               http/filter-success!
               :body :buildings))
      (http/route-cached-fetch data*)))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))

(defn link-to-room
  [room inner & {:keys [authorizers]
                 :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :room {:room-id (:id room)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/form-term-filter-component]
     [routing/select-component
      :label "Building"
      :query-params-key :building_id
      :options (cons [nil "(any)"]
                     (->> @buildings-data*
                          (map #(do [(:id %) (:name %)]))))]
     [routing/select-component
      :label "General"
      :query-params-key :general
      :options {nil "(any value)"
                "yes" "yes"
                "no" "no"}]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [room]
  (let [room-name (cond-> (:name room)
                    (:is_general room) (str " (general)"))
        inner-comp [:td {:key :code}
                    [link-to-room room [:span room-name]
                     :authorizers [auth/admin-scopes?]]]]
    (cond->> inner-comp
      (:is_general room) (vector :i))))

(defn building-link-th-component []
  [:th {:key :building_name} "Building"])

(defn building-link-td-component [row]
  [:td [:a {:href (path :building {:building-id (:building_id row)})}
        [:em (->> @buildings-data*
                  (detect #(= (:id %) (:building_id row)))
                  :name)]]
   ""])

(defn items-count-th-component []
  [:th.text-left {:key :items_count} "# Items"])

(defn items-count-td-component [room]
  [:td.text-left {:key :items_count} (:items_count room)])

;;;;;

(defn rooms-thead-component [more-cols]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    (for [[idx col] (map-indexed vector more-cols)]
      ^{:key idx} [col])]])

(defn room-row-component [room more-cols]
  ^{:key (:id room)}
  [:tr.room {:key (:id room)}
   [:td {:key :index} (:index room)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col room])])

(defn core-table-component [hds tds rooms]
  (if-let [rooms (seq rooms)]
    [:table.rooms.table.table-striped.table-sm
     [rooms-thead-component hds]
     [:tbody
      (let [page (:page @current-query-paramerters-normalized*)
            per-page (:per-page @current-query-paramerters-normalized*)]
        (doall (for [room rooms]
                 ^{:key (:id room)}
                 [room-row-component room tds])))]]
    [:div.alert.alert-warning.text-center "No (more) rooms found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :rooms)]))

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
      [:h3 "@buildings-data*"]
      [:pre (with-out-str (pprint @buildings-data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component {:did-change clean-and-fetch}]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [name-th-component
     building-link-th-component
     items-count-th-component]
    [name-td-component
     building-link-td-component
     items-count-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.rooms
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]]
   [:h1 [icons/rooms] " Rooms"]
   [main-page-content-component]])
