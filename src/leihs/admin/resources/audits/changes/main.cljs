(ns leihs.admin.resources.audits.changes.main
  (:require
   [accountant.core :as accountant]
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.audits.changes.shared :refer [default-query-params]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent :refer [reaction]]))

;;; data ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce data* (reagent/atom {}))

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge default-query-params (:query-params-raw @routing/state*) query-params)))

(defn fetch-changes [& _]
  (http-client/route-cached-fetch data* :reload true))

;;; meta ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tables*
  (reaction
   (let [query-param-table (some-> @routing/state* :query-params-raw :table presence)
         meta-tables (some-> @data*
                             (get-in [(:route @routing/state*) :meta :tables])
                             seq)
         tables (->> (concat [] meta-tables [query-param-table])
                     (map presence) (filter identity) distinct sort
                     (map (fn [t] [t t])))]
     (concat [["(any)" ""]]
             tables))))

;;; filters ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-filter-component []
  (let [disabled* (reaction (-> @tables* empty?))]
    (fn []
      [:div.form-group.m-2
       [:label {:for :table}
        [:span "Table name " [:small.text_monspache "(table)"]]]
       [:select#table.form-control
        {:value (:table (merge default-query-params
                               (:query-params-raw @routing/state*)))
         :disabled @disabled*
         :on-change (fn [e]
                      (let [val (or (-> e .-target .-value presence) "")]
                        (accountant/navigate! (page-path-for-query-params
                                               {:page 1
                                                :table val}))))}
        (for [[n v] @tables*]
          ^{:key n} [:option {:value v} n])]])))

(defn tg-op-filter-component []
  [:div.form-group.m-2
   [:label {:for :tg-op}
    [:span "Operation " [:small.text_monspache "(tg-op)"]]]
   [:select#tg-op.form-control
    {:value (:tg-op (merge default-query-params
                           (:query-params-raw @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                           {:page 1
                                            :tg-op val}))))}
    (for [[n v] (->> ["" "DELETE" "INSERT" "UPDATE"]
                     (map (fn [op] [op op])))]
      ^{:key n} [:option {:value v} n])]])

(defn filter-component []
  [filter/container
   [:<>
    [filter/delayed-query-params-input-component
     :label "Search in changed data"
     :query-params-key :term
     :input-options {:placeholder "fuzzy term"}]
    [filter/delayed-query-params-input-component
     :label "TXID"
     :query-params-key :txid
     :input-options {:placeholder "transaction id"}]
    [filter/delayed-query-params-input-component
     :label "Primary key"
     :query-params-key :pkey]
    [table-filter-component]
    [tg-op-filter-component]
    [filter/form-per-page]
    [filter/reset :default-query-params default-query-params]]])

;;; table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn thead-component [hds]
  [:tr
   [:th {:key :timestamp} "Timestamp"]
   [:th {:key :txid} "TX ID"]
   [:th {:key :pkey} "Pkey"]
   [:th {:key :table} "Table"]
   [:th {:key :tg-op} "Operation"]
   [:th {:key :changed-attributes} "Changed attributes"]
   [:th {:key :request}]
   [:th {:key :change}]
   (for [[idx hd] (map-indexed vector hds)]
     ^{:key idx} [hd])])

(defn row-component [change tds]
  [:tr.user
   {:key (:id change)}
   [:td.text-monospace.timestamp (:created_at change)]
   [:td [components/truncated-id-component (:txid change) :key :txid]]
   [:td [components/truncated-id-component (:pkey change) :key :pkey]]
   [:td.table-name (:table_name change)]
   [:td.tg-op (:tg_op change)]
   [:td.changed-attributes
    {:style {:max-width "20em"}}
    (->> change :changed_attributes (map str) (join ", "))]
   [:td.request
    (when (:has_request change)
      [:a {:href (path :audited-request {:request-id (:request-id change)})}
       [:span [icons/view] " Request "]])]
   [:td.change
    [:a {:href (path :audited-change {:audited-change-id (:id change)})}
     [:span [icons/view] " Change "]]]
   (for [[idx col] (map-indexed vector tds)]
     ^{:key idx} [col change])])

(defn table-component [changes hds tds]
  [table/container
   {:className "audited-changes"
    :actions [table/toolbar]
    :header
    [thead-component hds]
    :body
    (doall (for [change changes]
             (row-component change tds)))}])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:hr]
     [:div
      [:h3 "@tables*"]
      [:pre (with-out-str (pprint @tables*))]]
     [:div.data*
      [:h3 "@data"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:article.audited-changes-page
   [:header.my-5
    [:h1 [icons/arrow-right-arrow-left] " Audited Changes "]]
   [:section
    [filter-component]
    [routing/hidden-state-component
     {:did-change fetch-changes}]
    (if-not (contains? @data* (:route @routing/state*))
      [wait-component]
      (if-let [changes (-> @data* (get (:route @routing/state*) {}) :changes seq)]
        [table-component changes]
        [:div.alert.alert-warning.text-center "No (more) audited-changes found."]))
    [debug-component]]])
