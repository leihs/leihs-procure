(ns leihs.admin.resources.inventory-pools.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.json :as json]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [leihs.admin.utils.seq :as seq :refer [with-index]]
    [leihs.admin.resources.inventory-pools.shared :as shared]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)
                       :order (some-> @routing/state* :query-params
                                      :order clj->js json/to-json)))))

(def current-url* (reaction (:url @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def fetch-inventory-pools-id* (reagent/atom nil))

(def data* (reagent/atom {}))

(defn fetch-inventory-pools []
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
                                       :title "Fetch Inventory-Pools"
                                       :handler-key :inventory-pools
                                       :retry-fn #'fetch-inventory-pools}
                                      :chan resp-chan)]
            (reset! fetch-inventory-pools-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-inventory-pools-id*) ;still the most recent request
                             (= url @current-url*)) ;query-params have still not changed yet
                    (let [body (-> resp :body)
                          page (:page normalized-query-params)
                          per-page (:per-page normalized-query-params)
                          offset (* per-page (- page 1))
                          body-with-indexed-inventory-pools (update-in body [:inventory-pools] (partial with-index offset))]
                      (swap! data* assoc url body-with-indexed-inventory-pools))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-inventory-pools)
  (swap! state/global-state*
         assoc :inventory-pools-query-params @current-query-paramerters-normalized*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.sr-only {:for :inventory-pools-search-term} "Search term"]
   [:input#inventory-pools-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :term val}))))}]])



(defn form-is-active-filter []
  (let [active (or (-> @current-query-paramerters-normalized* :is-active presence) "all")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :inventory-pools-filter-type} "Active"]
     [:select#inventory-pools-filter-type.form-control
      {:value active
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :is-active val}))))}
      (for [t ["all" "active" "inactive"]]
        [:option {:key t :value t} t])]]))

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :inventory-pools-filter-per-page} "Per page"]
     [:select#inventory-pools-filter-per-page.form-control
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
   [:label.sr-only {:for :inventory-pools-filter-reset} "Reset"]
   [:a#inventory-pools-filter-reset.btn.btn-warning
    {:href (page-path-for-query-params shared/default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-term-filter]
    [form-is-active-filter]
    [form-per-page]
    [form-reset]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pools-thead-component [& [more-cols]]
  [:thead
   [:tr
    [:th "Index"]
    [:th "#Users " [:a {:href (page-path-for-query-params
                                {:order (-> [[:users_count :desc] [:id :asc]]
                                            clj->js json/to-json)})} "↓"]]
    [:th "Active"]
    [:th "Name " [:a {:href (page-path-for-query-params
                                {:order (-> [[:name :asc] [:id :asc]]
                                            clj->js json/to-json)})} "↓"]]
    (for [col more-cols]
      col)]])

(defn link-to-inventory-pool [inventory-pool inner]
  [:a {:href (path :inventory-pool {:inventory-pool-id (:id inventory-pool)})}
   inner])

(defn inventory-pool-row-component [inventory-pool more-cols]
  [:tr.inventory-pool {:key (:id inventory-pool)}
   [:td (link-to-inventory-pool inventory-pool (:index inventory-pool))]
   [:td (:users_count inventory-pool)]
   [:td [:p {:style {:font-family "monospace"}}
         (str (:is_active inventory-pool))]]
   [:td (link-to-inventory-pool inventory-pool (:name inventory-pool))]
   (for [col more-cols]
     (col inventory-pool))])

(defn inventory-pools-table-component [& [hds tds]]
  (if-not (contains? @data* @current-url*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]
     [:span.sr-only "Please wait"]]
    (if-let [inventory-pools (-> @data* (get  @current-url* {}) :inventory-pools seq)]
      [:table.table.table-striped.table-sm
       [inventory-pools-thead-component hds]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [inventory-pool inventory-pools]
                   (inventory-pool-row-component inventory-pool tds))))]]
      [:div.alert.alert-warning.text-center "No (more) inventory-pools found."])))

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
   [inventory-pools-table-component]
   [pagination-component]
   [debug-component]])

(defn page []
  [:div.inventory-pools
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)]
     [(breadcrumbs/inventory-pool-add-li)])
   [:h1 icons/inventory-pools " Inventory-Pools"]
   [main-page-content-component]])
