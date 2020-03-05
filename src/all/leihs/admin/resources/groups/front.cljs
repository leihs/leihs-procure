(ns leihs.admin.resources.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [leihs.admin.utils.seq :refer [with-index]]
    [leihs.admin.resources.groups.shared :as shared]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:url @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def fetch-groups-id* (reagent/atom nil))

(def data* (reagent/atom {}))

(defn fetch-groups []
  "Fetches the the currernt url with accept/json
  after 1/5 second timeout if query-params have not changed in the meanwhile
  yet and stores the result in the map data* under this url."
  (let [url @current-url*
        normalized-query-params @current-query-paramerters-normalized*]
    (go (<! (timeout 200))
        (when (= url @current-url*)
          (let [resp-chan (async/chan)
                id (requests/send-off {:url url
                                       :method :get}
                                      {:modal false
                                       :title "Fetch Groups"
                                       :handler-key :groups
                                       :retry-fn #'fetch-groups}
                                      :chan resp-chan)]
            (reset! fetch-groups-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-groups-id*) ;still the most recent request
                             (= url @current-url*)) ;query-params have still not changed yet
                    (let [body (-> resp :body)
                          page (:page normalized-query-params)
                          per-page (:per-page normalized-query-params)
                          offset (* per-page (- page 1))
                          body-with-indexed-groups (update-in body [:groups] (partial with-index offset))]
                      (swap! data* assoc url body-with-indexed-groups))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-groups)
  (swap! state/global-state*
         assoc :groups-query-params @current-query-paramerters-normalized*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.sr-only {:for :groups-search-term} "Search term"]
   [:input#groups-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :term val}))))}]])

(defn form-type-filter []
  (let [type (or (-> @current-query-paramerters-normalized* :type presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :groups-filter-type} "Type"]
     [:select#groups-filter-type.form-control
      {:value type
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :type val}))))}
      (for [t ["any" "org" "manual"]]
        [:option {:key t :value t} t])]]))

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :groups-filter-per-page} "Per page"]
     [:select#groups-filter-per-page.form-control
      {:value per-page
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "12")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :per-page val}))))}
      (for [p [12 25 50 100 250 500 1000]]
        [:option {:key p :value p} p])]]))

(defn form-reset []
  [:div.form-group.mt-2
   [:label.sr-only {:for :groups-filter-reset} "Reset"]
   [:a#groups-filter-reset.btn.btn-warning
    {:href (page-path-for-query-params shared/default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-term-filter]
    [form-type-filter]
    [form-per-page]
    [form-reset]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-thead-component [& [more-cols]]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    [:th {:key :count_users} "# Users"]
    [:th {:key :org_id} "Org id"]
    [:th {:key :name} "Name"]
    (for [col more-cols]
      col)]])

(defn link-to-group [group inner]
  [:a {:href (path :group {:group-id (:id group)})}
   inner])

(defn group-row-component [group more-cols]
  [:tr.group {:key (:id group)}
   [:td {:key :index} (link-to-group group (:index group))]
   [:td {:key :users_count} (:count_users group)]
   [:td {:key :org_id}
    (link-to-group group
                   [:p {:style {:font-family "monospace"}}
                    (:org_id group)])]
   [:td {:key :name} (link-to-group group (:name group))]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col group])])

(defn groups-table-component [& [hds tds]]
  (if-not (contains? @data* @current-url*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]
     [:span.sr-only "Please wait"]]
    (if-let [groups (-> @data* (get  @current-url* {}) :groups seq)]
      [:table.groups.table.table-striped.table-sm
       [groups-thead-component hds]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [group groups]
                   (group-row-component group tds))))]]
      [:div.alert.alert-warning.text-center "No (more) groups found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pagination-component []
  [:div.clearfix.mt-2.mb-2
   (let [page (dec (:page @current-query-paramerters-normalized*))]
     [:div.float-left
      [:a.btn.btn-primary.btn-sm
       {:class (when (< page 1) "disabled")
        :href (page-path-for-query-params {:page page})}
       [:i.fas.fa-arrow-circle-left] " Previous " ]])
   [:div.float-right
    [:a.btn.btn-primary.btn-sm
     {:href (page-path-for-query-params
              {:page (inc (:page @current-query-paramerters-normalized*))})}
     " Next " [:i.fas.fa-arrow-circle-right]]]])

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
      [:pre (with-out-str (pprint @data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component
    {:did-mount escalate-query-paramas-update
     :did-update escalate-query-paramas-update}]
   [filter-component]
   [pagination-component]
   [groups-table-component]
   [pagination-component]
   [debug-component]])

(defn page []
  [:div.groups
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/groups-li)]
     [(breadcrumbs/group-add-li)])
   [:h1 "Groups"]
   [main-page-content-component]])
