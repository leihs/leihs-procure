(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.edit :as edit]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as users]
   [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front]
   [react-bootstrap :as react-bootstrap :refer [Button Table]]
   [reagent.core :as reagent]
   [taoensso.timbre]))

;;; suspension ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce data* (reagent/atom nil))

(defn suspension-section []
  [:div.row
   [:div.col-md-6
    [:hr]
    [:section#suspension
     [:h2 " Suspension "]
     [:div
      (let [suspension-path (path :inventory-pool-delegation-suspension
                                  (some-> @routing/state* :route-params))]
        [:<>
         [routing/hidden-state-component
          {:did-mount
           #(go (reset! data* (<! (suspension-core/fetch-suspension< suspension-path))))}]
         [suspension-core/suspension-component @data*
          :update-handler
          #(go (reset! data* (<! (suspension-core/put-suspension< suspension-path %))))]])]]]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def show* (reagent/atom false))

(defn check-user-chosen []
  (when (contains?
         (get @routing/state* :query-params) :user-uid)
    (reset! show* true)))

(defn edit-delegation []
  [:<>
   [:> Button
    {:className ""
     :onClick #(do
                 (check-user-chosen)
                 (reset! show* true))}
    "Edit"]])

(defn edit-delegation-dialog []
  [edit/dialog {:show @show*
                :onHide #(reset! show* false)}])

(defn delegation-info-section []
  [:section.delegation
   (if-let [delegation (get @delegation/data* @delegation/id*)]
     [:div
      [:> Table {:striped true :borderless true}
       [:thead
        [:tr
         [:th "Property"]
         [:th "Value"]]]
       [:tbody
        [:tr.name [:td "Name"] [:td.name (:name delegation)]]
        [:tr.responsible-user
         [:td "Responsible user"]
         [:td.responsible-user
          [users/user-inner-component
           (:responsible_user delegation)]]]
        [:tr.users-count
         [:td "Number of users"]
         [:td.users-count (:users_count delegation)]]
        [:tr.direct-users-count
         [:td "Number of direct users"]
         [:td.direct-users-count (:direct_users_count delegation)]]
        [:tr.groups-count
         [:td "Number of groups"]
         [:td.groups-count (:groups_count delegation)]]
        [:tr.protected
         [:td "Protected"]
         [:td (if (:pool_protected delegation)
                [:span.text-success "yes"]
                [:span.text-warning "no"])]]
        [:tr.contracts-count-open-per-pool
         [:td "Number of contracts open in pool "]
         [:td.contracts-count-open-per-pool (:contracts_count_open_per_pool delegation)]]
        [:tr.contracts-count-per-pool
         [:td "Number of contracts in pool "]
         [:td.contracts-count-per-pool (:contracts_count_per_pool delegation)]]
        [:tr.contracts-count
         [:td "Number of contracts total "]
         [:td.contracts-count (:contracts_count delegation)]]
        [:tr.other-pools
         [:td "Used in the following other pools"]
         [:td [:ul
               (doall (for [pool (:other_pools delegation)]
                        (let [inner [:span "in " (:name pool)]]
                          (if-not (:is_admin  @leihs.core.user.front/state*)
                            [:span inner]
                            [:li {:key (:id pool)}
                             [:a {:href (path :inventory-pool-delegation
                                              {:inventory-pool-id (:id pool)
                                               :delegation-id (:id delegation)})}
                              inner]]))))]]]
        [:tr.created
         [:td "Created "]
         [:td.created (-> delegation :created_at humanize-datetime-component)]]]]]
     [wait-component])
   [edit-delegation]
   [edit-delegation-dialog]])

(defn page []
  [:article.delegation.my-5
   [delegation/header]
   [delegation/tabs]
   [delegation-info-section]
   [suspension-section]
   [delegation/debug-component]])
