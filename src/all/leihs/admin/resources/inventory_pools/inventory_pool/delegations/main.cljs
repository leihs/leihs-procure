(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.components :as components]
    [leihs.admin.defaults :as defaults]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared :refer [default-query-params]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as users]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-query-paramerters-normalized*
  (reaction (merge default-query-params
           @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defn fetch-delegations []
  (def fetch-delegations-id* (reagent/atom nil))
  (let [query-paramerters @current-query-paramerters-normalized*]
    (go (<! (timeout 100))
        (when (= query-paramerters @current-query-paramerters-normalized*)
          (let [resp-chan (async/chan)
                url (:url @routing/state*)
                id (requests/send-off {:url (path :inventory-pool-delegations
                                                  {:inventory-pool-id @inventory-pool/id*})
                                       :method :get
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
                    (swap! data* assoc url (:body resp))))))))))

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


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-membership-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.mr-1 {:for :users-membership} " Membership "]
   [:select#users-membership.form-control
    {:value (:membership (merge default-query-params
                                (:query-params @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate!
                      (routing/current-path-for-query-params
                        default-query-params
                        {:page 1 :membership val}))))}
    (doall (for [[k n] {"any" "members and non-members"
                        "non" "non-members"
                        "member" "members"}]
             [:option {:key k :value k} n] ))]])

(defn filter-form []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [routing/form-term-filter-component
      {:default-query-params default-query-params}]
     [form-membership-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])


;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delegations-thead-component []
  [:thead
   [:tr
    [:th.text-left "Name"]
    [:th.text-left " Responsible user "]
    [:th.text-right " # Contracts "]
    [:th.text-right " # Users"]
    [:th.text-right " # Direct users "]
    [:th.text-right " # Groups"]
    [:th.text-right " # Pools"]
    [:th.text-center " Protected "]
    [:th.text-center " Action "]]])

(defn link-to-delegation [id inner-component]
  [:a {:href (path :inventory-pool-delegation
                   {:inventory-pool-id @inventory-pool/id*
                    :delegation-id id})}
   inner-component])

(defn remove-component [delegation]
  [:button.btn.btn-warning
   icons/delete " Remove "])

(defn name-td-component [id delegation]
  [:td.name.text-left
   (let [inner [:span (:firstname delegation)]]
     (if (:member delegation)
       (link-to-delegation id inner)
       inner))])

(defn users-count-td-component [id delegation]
  [:td.users-count.text-right
   (-> delegation :users_count)])

(defn direct-users-count-td-component [id delegation]
  [:td.direct-users-count.text-right
   (-> delegation :direct_users_count)])

(defn groups-count-td-component [id delegation]
  [:td.groups-count.text-right
   (-> delegation :groups_count)])

(defn add-delegation [delegation]
  (let [resp-chan (async/chan)
        _ (requests/send-off {:url (path :inventory-pool-delegation
                                          {:inventory-pool-id @inventory-pool/id*
                                           :delegation-id (:id delegation)})
                               :method :put}
                              {:modal true
                               :title "Add Delegation" }
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (< (:status resp) 300)
            (fetch-delegations))))))

(defn delete-delegation [delegation]
  (let [resp-chan (async/chan)
        _ (requests/send-off {:url (path :inventory-pool-delegation
                                         {:inventory-pool-id @inventory-pool/id*
                                          :delegation-id (:id delegation)})
                              :method :delete}
                             {:modal true
                              :title "Delete Delegation" }
                             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (< (:status resp) 300)
            (fetch-delegations))))))

(defn action-td-component
  [{member :member id :id protected :protected
    pools-count :pools_count :as delegation}]
  [:td.text-center
   (if-not member
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (add-delegation delegation))}
      [:button.btn.btn-primary.btn-sm
       icons/add " Add "]]
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (delete-delegation delegation))}
      (if (> pools-count 1)
        [:button.btn.btn-warning.btn-sm
         icons/delete " Remove "]
        [:button.btn.btn-danger.btn-sm
         icons/delete " Delete "])])])

(defn delegation-row-component [{id :id :as delegation}]
  [:tr.delegation {:key (:id delegation)}
   [name-td-component id delegation]
   [:td.responsible-user
    [users/user-inner-component (:responsible_user delegation)]]
   [:td.contracs-count.text-right [:span (:contracts_count_open_per_pool delegation)
                                   " / " (:contracts_count_per_pool delegation)
                                   " / " (:contracts_count delegation)]]
   [users-count-td-component id delegation]
   [direct-users-count-td-component id delegation]
   [groups-count-td-component id delegation]
   [:td.text-right
    (let [pools-count (:pools_count delegation)]
      (if (> pools-count 1)
        [:strong.text-warning pools-count]
        [:span.text-success pools-count]))]
   [:td.text-center (if (:protected delegation)
                      [:span.text-success "yes"]
                      [:span.text-warning "no"])]
   [action-td-component delegation]])

(defn delegations-table-component []
  (let [current-url (:url @routing/state*)]
    (if-not (contains? @data* current-url)
      [wait-component]
      (if-let [delegations (-> @data* (get  current-url  {}) :delegations seq)]
        [:table.table.table-striped.table-sm
         [delegations-thead-component]
         [:tbody
          (let [page (:page @current-query-paramerters-normalized*)
                per-page (:per-page @current-query-paramerters-normalized*)]
            (for [[k delegation] (map-indexed vector delegations)]
              ^{:key k} [delegation-row-component delegation]))]]
        [:div.alert.alert-warning.text-center "No (more) delegations found."]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div.delegations
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn breadcrumbs []
  [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]])

(defn page []
  [:div.delegations
   [breadcrumbs]
   [current-query-params-component]
   [:h1
    [:span "Delegations in the Inventory-Pool "]
    [inventory-pool/name-link-component]]
   [filter-form]
   [routing/pagination-component]
   [delegations-table-component]
   [routing/pagination-component]
   [debug-component]])
