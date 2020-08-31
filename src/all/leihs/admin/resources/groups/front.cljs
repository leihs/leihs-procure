(ns leihs.admin.resources.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.defaults :as defaults]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [wait-component]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [leihs.admin.utils.seq :as seq]
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
                          offset (* per-page (- page 1))]
                      (swap! data* assoc url
                             (-> body
                                 (update-in [:groups] (partial seq/with-index offset))
                                 (update-in [:groups] (partial seq/with-page-index))
                                 (update-in [:groups] #(into [] %)))))))))))))

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
   [:label {:for :groups-search-term} "Search term"]
   [:input#groups-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :term val}))))}]])

(defn form-including-user-filter []
[:div.form-group.ml-2.mr-2.mt-2
   [:label {:for :groups-including-user} "Including User"]
   [:input#groups-including-user.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "ID or E-Mail Address"
     :value (or (-> @current-query-paramerters-normalized* :including-user presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :including-user val}))))}]])

(defn form-org-filter []
  (let [org-id (some-> @current-query-paramerters-normalized* :org_id)]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :groups-filter-org-id} "Org id"]
     [:input#groups-filter-org-id.form-control
      {:type :text
       :value org-id
       :placeholder "org_id or true or false"
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :org_id val}))))}]]))

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [:div.col-auto [form-term-filter]]
    [:div.col-auto [form-including-user-filter]]
    [:div.col-auto [form-org-filter]]
    [:div.col-auto [routing/form-per-page-component]]
    [:div.col-auto [routing/form-reset-component]]]]])


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
    [wait-component]
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
   [routing/pagination-component]
   [groups-table-component]
   [routing/pagination-component]
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
