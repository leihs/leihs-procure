(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.main
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as pool-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.create :as create]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.shared :refer [default-query-params]]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as users]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [fetch-route* wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Table]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-query-parameters-normalized*
  (reaction (merge default-query-params
                   @current-query-parameters*)))

(def current-route*
  (reaction
   (when (-> @routing/state*
             :route-params
             :inventory-pool-id)

     (path :inventory-pool-delegations
           (:route-params @routing/state*)))))

(def data* (reagent/atom nil))

(defn fetch []
  (http-client/route-cached-fetch data* {:route @fetch-route*
                                         :reload true}))

(defn filter-section []
  [filter/container
   [:<>
    [filter/form-term-filter-component :placeholder "Name of the Delegation"]
    [filter/form-including-user]
    [filter/select-component
     :label "Membership"
     :query-params-key :membership
     :options {"member" "members"
               "non" "non-members"
               "any" "members and non-members"}]
    [filter/form-suspension]
    [filter/form-per-page]
    [filter/reset]]])

(defn delegations-thead []
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
    [:th.text-center " Action "]
    [:th.text-center " Suspension "]]])

(defn link-to-delegation [id inner-component]
  [:a {:href (path :inventory-pool-delegation
                   {:inventory-pool-id @pool-core/id*
                    :delegation-id id})}
   inner-component])

(defn name-td [id delegation]
  [:td.name.text-left
   (let [inner [:span (:firstname delegation)]]
     (if (:member delegation)
       (link-to-delegation id inner)
       inner))])

(defn users-count-td [id delegation]
  [:td.users-count.text-right
   (-> delegation :users_count)])

(defn direct-users-count-td [id delegation]
  [:td.direct-users-count.text-right
   (-> delegation :direct_users_count)])

(defn groups-count-td [id delegation]
  [:td.groups-count.text-right
   (-> delegation :groups_count)])

(defn add-or-remove-delegation [method delegation]
  (go (when (some->
             {:chan (async/chan)
              :url (path :inventory-pool-delegation
                         {:inventory-pool-id @pool-core/id*
                          :delegation-id (:id delegation)})
              :method method}
             http-client/request :chan <!
             http-client/filter-success!)
        (fetch))))

(defn action-td
  [{member :member id :id protected :pool_protected
    pools-count :pools_count :as delegation}]
  [:td.text-center
   (if-not member
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (add-or-remove-delegation :put delegation))}
      [:button.btn.btn-primary.btn-sm
       [icons/add] " Add "]]
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (add-or-remove-delegation :delete delegation))}
      (if (> pools-count 1)
        [:button.btn.btn-warning.btn-sm
         [icons/delete] " Remove "]
        [:button.btn.btn-danger.btn-sm
         [icons/delete] " Delete "])])])

(defn suspension-td [delegation]
  [:td.suspension.text-center
   (suspension/suspension-component
    (:suspension delegation)
    :compact true
    :update-handler (fn [updated]
                      (go (let [data (<! (suspension/put-suspension<
                                          (path :inventory-pool-delegation-suspension
                                                {:inventory-pool-id @pool-core/id*
                                                 :delegation-id (:id delegation)})
                                          updated))]
                            (swap! data* assoc-in
                                   [@fetch-route* :delegations
                                    (:page-index delegation) :suspension] data)))))])

(defn delegation-row [{id :id :as delegation}]
  [:tr.delegation {:key (:id delegation)}
   [name-td id delegation]
   [:td.responsible-user
    [users/user-inner-component (:responsible_user delegation)]]
   [:td.contracs-count.text-right [:span (:contracts_count_open_per_pool delegation)
                                   " / " (:contracts_count_per_pool delegation)
                                   " / " (:contracts_count delegation)]]
   [users-count-td id delegation]
   [direct-users-count-td id delegation]
   [groups-count-td id delegation]
   [:td.text-right
    (let [pools-count (:pools_count delegation)]
      (if (> pools-count 1)
        [:strong.text-warning pools-count]
        [:span.text-success pools-count]))]
   [:td.text-center (if (:pool_protected delegation)
                      [:span.text-success "yes"]
                      [:span.text-warning "no"])]
   [action-td delegation]
   [suspension-td delegation]])

(defn delegations-table []
  (if-not (contains? @data* @fetch-route*)
    [wait-component]
    (if-let [delegations (-> @data*
                             (get @fetch-route*)
                             :delegations seq)]
      [:> Table {:striped true
                 :hover true
                 :borderless true
                 :className "border-top border-bottom"}

       [delegations-thead]
       [:tbody
        (let [page (:page @current-query-parameters-normalized*)
              per-page (:per-page @current-query-parameters-normalized*)]
          (for [[k delegation] (map-indexed vector delegations)]
            ^{:key k} [delegation-row delegation]))]]
      [:div.alert.alert-info.text-center "No (more) delegations found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div.delegations
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(pool-core/fetch)
     :did-change #(fetch)}]

   [:article.delegations
    [pool-core/header]

    [:section.mb-5
     [pool-core/tabs]
     [filter-section]
     [table/toolbar [create/button]]
     [delegations-table]
     [table/toolbar [create/button]]
     [create/dialog]]

    [debug-component]]])
