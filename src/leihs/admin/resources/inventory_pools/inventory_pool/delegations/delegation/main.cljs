(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.main
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as core]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.edit :as edit]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as suspension-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as users]
   [leihs.admin.utils.misc :as utils :refer [humanize-datetime-component
                                             wait-component]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front]
   [react-bootstrap :as react-bootstrap :refer [Table]]
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

(defn delegation-info-section []
  [:section.delegation
   [:div
    [:> Table {:striped true :borderless true}
     [:thead
      [:tr
       [:th "Property"]
       [:th "Value"]]]
     [:tbody
      [:tr.name [:td "Name"] [:td.name (:name @core/delegation*)]]
      [:tr.responsible-user
       [:td "Responsible user"]
       [:td.responsible-user
        [users/user-inner-component
         (:responsible_user @core/delegation*)]]]
      [:tr.users-count
       [:td "Number of users"]
       [:td.users-count (:users_count @core/delegation*)]]
      [:tr.direct-users-count
       [:td "Number of direct users"]
       [:td.direct-users-count (:direct_users_count @core/delegation*)]]
      [:tr.groups-count
       [:td "Number of groups"]
       [:td.groups-count (:groups_count @core/delegation*)]]
      [:tr.protected
       [:td "Protected"]
       [:td (if (:pool_protected @core/delegation*)
              [:span.text-success "yes"]
              [:span.text-warning "no"])]]
      [:tr.contracts-count-open-per-pool
       [:td "Number of contracts open in pool "]
       [:td.contracts-count-open-per-pool (:contracts_count_open_per_pool @core/delegation*)]]
      [:tr.contracts-count-per-pool
       [:td "Number of contracts in pool "]
       [:td.contracts-count-per-pool (:contracts_count_per_pool @core/delegation*)]]
      [:tr.contracts-count
       [:td "Number of contracts total "]
       [:td.contracts-count (:contracts_count @core/delegation*)]]
      [:tr.other-pools
       [:td "Used in the following other pools"]
       [:td [:ul
             (doall (for [pool (:other_pools @core/delegation*)]
                      (let [inner [:span "in " (:name pool)]]
                        (if-not (:is_admin  @leihs.core.user.front/state*)
                          [:span inner]
                          [:li {:key (:id pool)}
                           [:a {:href (path :inventory-pool-delegation
                                            {:inventory-pool-id (:id pool)
                                             :delegation-id (:id @core/delegation*)})}
                            inner]]))))]]]
      [:tr.created
       [:td "Created "]
       [:td.created (-> @core/delegation* :created_at humanize-datetime-component)]]]]]])

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(core/fetch)}]

   [:article.delegation.my-5
    [core/header]

    [:section.info.mb-5
     [core/tabs]
     [delegation-info-section]
     [edit/button]
     [edit/dialog]]

    [:section.suspension.mb-5
     [suspension-section]]

    [core/debug-component]]])
