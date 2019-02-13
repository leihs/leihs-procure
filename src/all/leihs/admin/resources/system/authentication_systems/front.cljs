(ns leihs.admin.resources.system.authentication-systems.front
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
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as ass-breadcrumbs]
    [leihs.admin.resources.system.breadcrumbs :as system-breadcrumbs]

    [leihs.admin.utils.seq :refer [with-index]]
    [leihs.admin.resources.system.authentication-systems.shared :as shared]

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

(def fetch-authentication-systems-id* (reagent/atom nil))

(def data* (reagent/atom {}))

(defn fetch-authentication-systems []
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
                                       :title "Fetch Authentication-Systems"
                                       :handler-key :authentication-systems
                                       :retry-fn #'fetch-authentication-systems}
                                      :chan resp-chan)]
            (reset! fetch-authentication-systems-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-authentication-systems-id*) ;still the most recent request
                             (= url @current-url*)) ;query-params have still not changed yet
                    (let [body (-> resp :body)
                          page (:page normalized-query-params)
                          per-page (:per-page normalized-query-params)
                          offset (* per-page (- page 1))
                          body-with-indexed-authentication-systems (update-in body [:authentication-systems] (partial with-index offset))]
                      (swap! data* assoc url body-with-indexed-authentication-systems))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-authentication-systems)
  (swap! state/global-state*
         assoc :authentication-systems-query-params @current-query-paramerters-normalized*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :authentication-systems-filter-per-page} "Per page"]
     [:select#authentication-systems-filter-per-page.form-control
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
   [:label.sr-only {:for :authentication-systems-filter-reset} "Reset"]
   [:a#authentication-systems-filter-reset.btn.btn-warning
    {:href (page-path-for-query-params shared/default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-per-page]
    [form-reset]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authentication-systems-thead-component []
  [:thead
   [:tr
    [:th "Index"]
    [:th "Id"]
    [:th "Enabled"]
    [:th "Type"]
    [:th "Priority"]
    [:th "# Users"]
    [:th "Name"]]])

(defn link-to-authentication-system [authentication-system inner]
  [:a {:href (path :authentication-system {:authentication-system-id (:id authentication-system)})}
   inner])

(defn authentication-system-row-component [authentication-system]
  [:tr.authentication-system {:key (:id authentication-system)}
   [:td (link-to-authentication-system authentication-system (:index authentication-system))]
   [:td (link-to-authentication-system authentication-system (:id authentication-system))]
   [:td (-> authentication-system :enabled str)]
   [:td (:type authentication-system)]
   [:td (:priority authentication-system)]
   [:td (:count_users authentication-system)]
   [:td (link-to-authentication-system authentication-system (:name authentication-system))]])

(defn authentication-systems-table-component []
  (if-not (contains? @data* @current-url*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]
     [:span.sr-only "Please wait"]]
    (if-let [authentication-systems (-> @data* (get  @current-url* {}) :authentication-systems seq)]
      [:table.table.table-striped.table-sm
       [authentication-systems-thead-component]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [authentication-system authentication-systems]
                   (authentication-system-row-component authentication-system))))]]
      [:div.alert.alert-warning.text-center "No (more) authentication-systems found."])))

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
    {:will-mount escalate-query-paramas-update
     :did-update escalate-query-paramas-update}]
   [filter-component]
   [pagination-component]
   [authentication-systems-table-component]
   [pagination-component]
   [debug-component]])

(defn page []
  [:div.authentication-systems
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (ass-breadcrumbs/authentication-systems-li)]
     [(ass-breadcrumbs/authentication-system-add-li)])
   [:h1 "Authentication-Systems"]
   [main-page-content-component]
   ])
