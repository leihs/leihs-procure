(ns leihs.admin.resources.delegations.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters* (reaction (-> @state/routing-state* :query-params)))

(def default-query-parameters {:is_admin nil
                               :role "any"
                               :page 1
                               :per-page 12
                               :term ""
                               :type "any" })

(def current-query-paramerters-normalized*
  (reaction (merge default-query-parameters
           @current-query-paramerters*)))

(def fetch-delegations-id* (reagent/atom nil))
(def delegations* (reagent/atom {}))

(defn fetch-delegations []
  (let [query-paramerters @current-query-paramerters-normalized*]
    (go (<! (timeout 200))
        (when (= query-paramerters @current-query-paramerters-normalized*)
          (let [resp-chan (async/chan)
                id (requests/send-off {:url (path :delegations) :method :get
                                       :query-params query-paramerters}
                                      {:modal false
                                       :title "Fetch Delegations"
                                       :handler-key :delegations
                                       :retry-fn #'fetch-delegations}
                                      :chan resp-chan)]
            (reset! fetch-delegations-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-delegations-id*) ;still the most recent request
                             (= query-paramerters @current-query-paramerters-normalized*)) ;query-params have not changed yet
                    ;(reset! effective-query-paramerters* current-query-paramerters)
                    (swap! delegations* assoc query-paramerters (->> (-> resp :body :delegations)
                                                               (map-indexed (fn [idx u]
                                                                              (assoc u :key (:id u)
                                                                                     :c idx)))
                                                               ))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-delegations)
  (swap! state/global-state*
         assoc :delegations-query-params @current-query-paramerters-normalized*))

(defn current-query-params-component []
  (reagent/create-class
    {:component-did-mount escalate-query-paramas-update
     :component-did-update escalate-query-paramas-update
     :reagent-render
     (fn [_] [:div.current-query-parameters
              {:style {:display (if @state/debug?* :block :none)}}
              [:h3 "@current-query-paramerters-normalized*"]
              [components/pre-component @current-query-paramerters-normalized*]])}))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.sr-only {:for :delegations-search-term} "Search term"]
   [:input#delegations-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (path :delegations {}
                                                (merge @current-query-paramerters-normalized*
                                                       {:page 1
                                                        :term val})))))}]])

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :delegations-filter-per-page} "Per page"]
     [:select#delegations-filter-per-page.form-control
      {:value per-page
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "12")]
                      (accountant/navigate! (path :delegations {}
                                                  (merge @current-query-paramerters-normalized*
                                                         {:page 1
                                                          :per-page val})))))}
      (for [p [12 25 50 100 250 500 1000]]
        [:option {:key p :value p} p])]]))

(defn form-reset []
  [:div.form-group.mt-2.right
   [:label.sr-only {:for :delegations-filter-reset} "Reset"]
   [:a#delegations-filter-reset.btn.btn-warning
    {:href (path :delegations {} default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-form []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-term-filter]
    [form-per-page]
    [form-reset]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegations-thead-component []
  [:thead
   [:tr
    [:th]
    [:th "# Users"]
    [:th "# Contracts"]
    [:th "Name"]
    ]])

(defn link-to-delegation [id inner-component]
  [:a {:href (path :delegation {:delegation-id id})}
   inner-component])

(defn delegation-row-component [delegation]
  [:tr.delegation {:key (:key delegation)}
   [:td.index (link-to-delegation (:id delegation) [:span (:index delegation)])]
   [:td.count_users (:count_users delegation)]
   [:td.count_contracts (:count_contracts delegation)]
   [:td.name (link-to-delegation (:id delegation) [:span (:firstname delegation)])]])

(defn delegations-table-component []
  (if-not (contains? @delegations* @current-query-paramerters-normalized*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]
     [:span.sr-only "Please wait"]]
    (if-let [delegations (-> @delegations* (get  @current-query-paramerters-normalized* []) seq)]
      [:table.table.table-striped.table-sm
       [delegations-thead-component]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [delegation delegations]
                   (delegation-row-component
                     (assoc delegation :index (+ 1 (:c delegation) (* per-page (- page 1))))))))]]
      [:div.alert.alert-warning.text-center "No (more) delegations found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pagination-component []
  [:div.clearfix.mt-2.mb-2
   (let [page (dec (:page @current-query-paramerters-normalized*))]
     [:div.float-left
      [:a.btn.btn-primary.btn-sm
       {:class (when (< page 1) "disabled")
        :href (path :delegations {} (assoc @current-query-paramerters-normalized*
                                     :page page))}
       [:i.fas.fa-arrow-circle-left] " Previous " ]])
   [:div.float-right
    [:a.btn.btn-primary.btn-sm
     {:href (path :delegations {} (assoc @current-query-paramerters-normalized*
                                   :page (inc (:page @current-query-paramerters-normalized*))))}
     " Next " [:i.fas.fa-arrow-circle-right]]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div.delegations
      [:h3 "@delegations*"]
      [:pre (with-out-str (pprint @delegations*))]]]))

(defn page []
  [:div.delegations
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)]
     [(breadcrumbs/delegation-add-li)])
   [current-query-params-component]
   [:h1 "Delegations"]
   [filter-form]
   [pagination-component]
   [delegations-table-component]
   [pagination-component]
   [debug-component]])
