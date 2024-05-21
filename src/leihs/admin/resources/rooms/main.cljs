(ns leihs.admin.resources.rooms.main
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.rooms.room.core :as room :refer [buildings-data*]]
   [leihs.admin.resources.rooms.room.create :as create]
   [leihs.admin.resources.rooms.shared :as shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.core :refer [detect]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert Button]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-parameters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-parameters*)))

(def data* (reagent/atom {}))

(defn clean-and-fetch []
  (go (reset! buildings-data*
              (some->
               {:chan (async/chan)
                :url (path :buildings)}
               http/request :chan <!
               http/filter-success!
               :body :buildings))
      (http/route-cached-fetch data*)))

(defn link-to-room
  [room inner & {:keys [authorizers]
                 :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :room {:room-id (:id room)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [filter/container
   [:<>
    [filter/form-term-filter-component]
    [filter/select-component
     :label "Building"
     :query-params-key :building_id
     :options (cons [nil "(any)"]
                    (->> @buildings-data*
                         (map #(do [(:id %) (:name %)]))))]
    [filter/select-component
     :label "General"
     :query-params-key :general
     :options {nil "(any value)"
               "yes" "yes"
               "no" "no"}]
    [filter/form-per-page]
    [filter/reset]]])

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

(defn description-th-component []
  [:th {:key :description} "Description"])

(defn description-td-component [room]
  [:td {:key :description} (-> room :description)])

(defn building-link-th-component []
  [:th {:key :building_name} "Building"])

(defn building-link-td-component [row]
  [:td [:a {:href (path :building {:building-id (:building_id row)})}
        (->> @buildings-data*
             (detect #(= (:id %) (:building_id row))) :name)]])

(defn items-count-th-component []
  [:th.text-left {:key :items_count} "# Items"])

(defn items-count-td-component [room]
  [:td.text-left {:key :items_count} (:items_count room)])

;;;;;

(defn add-room-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:className "ml-3"
         :onClick #(reset! show true)}
        "Add Room"]
       [create/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn rooms-thead-component [more-cols]
  [:tr
   [:th {:key :index} "Index"]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col])])

(defn room-row-component [room more-cols]
  ^{:key (:id room)}
  [:tr.room {:key (:id room)}
   [:td {:key :index} (:index room)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col room])])

(defn core-table-component [hds tds rooms]
  (if-let [rooms (seq rooms)]
    [table/container
     {:className "rooms"
      :actions [table/toolbar [add-room-button]]
      :header [rooms-thead-component hds]
      :body
      (doall (for [room rooms]
               ^{:key (:id room)}
               [room-row-component room tds]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No (more) rooms found."]))

(defn table-component [hds tds]
  (if (empty? @data*)
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
      [:h3 "@current-query-parameters-normalized*"]
      [:pre (with-out-str (pprint @current-query-parameters-normalized*))]]
     [:div
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]
     [:div
      [:h3 "@buildings-data*"]
      [:pre (with-out-str (pprint @buildings-data*))]]]))

(defn page []
  [:article.rooms
   [:header.my-5
    [:h1 [icons/rooms] " Rooms"]]
   [:section
    [routing/hidden-state-component
     {:did-change clean-and-fetch}]
    [filter-component]
    [table-component
     [name-th-component
      description-th-component
      building-link-th-component
      items-count-th-component]
     [name-td-component
      description-td-component
      building-link-td-component
      items-count-td-component]]
    [debug-component]]])
