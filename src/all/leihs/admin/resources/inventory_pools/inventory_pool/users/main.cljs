(ns leihs.admin.resources.inventory-pools.inventory-pool.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.roles.core :as roles]
    [leihs.admin.common.roles.components :refer [roles-component fetch-roles< put-roles<]]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.shared :refer [default-query-params]]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.main :as suspension]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.resources.users.user.core :as user2]
    [leihs.admin.resources.users.user.shared :as user]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
    [leihs.admin.utils.regex :as regex]

    ["date-fns" :as date-fns]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))


(def inventory-pool-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :inventory-pool_users_count)))

(def current-query-params*
  (reaction (merge default-query-params
                   @users/current-query-paramerters-normalized*)))


;### user #####################################################################

(defn user-th-component []
  [:th "User"])

(defn user-inner-component [user]
  [:a {:href (path :inventory-pool-user
                   {:user-id (:id user)
                    :inventory-pool-id @inventory-pool/id*})}
   [:ul.list-unstyled
    (for [[idx item] (map-indexed vector (user2/fullname-some-uid-seq user))]
      ^{key idx} [:li {:key idx} item])]])

(defn user-td-component [user]
  [:td.user [user-inner-component user] ])


;### roles ####################################################################

(defn roles-th-component []
  [:th {:key :roles} " Roles "])

(defn roles-td-component [user]
  [:td {:key :roles} [roles-component
                      (->> roles/hierarchy
                           (map (fn [rk] [rk (or (-> user :direct_roles rk)
                                                 (-> user :groups_roles rk))]))
                           (into {}))
                      :compact true]])


;### direct roles #############################################################

(defn direct-roles-th-component []
  [:th {:key :direct-roles} " Direct roles "])

(defn direct-roles-update-handler [roles user]
  (go (swap! users/data* assoc-in
             [(:url @routing/state*) :users (:page-index user) :direct_roles]
             (<! (put-roles<
                   (path :inventory-pool-user-direct-roles
                         {:inventory-pool-id @inventory-pool/id*
                          :user-id (:id user)})
                   roles)))))

(defn direct-roles-td-component [user]
  [:td.direct-roles {:key :direct-roles}
   [roles-component
    (get user :direct_roles)
    :compact true
    :update-handler #(direct-roles-update-handler % user)]])


;### groups roles #############################################################

(defn groups-roles-th-component []
  [:th {:key :groups-roles} " Roles via groups "])

(defn groups-roles-td-component [user]
  (let [has-a-role? (some->> user :groups_roles vals (reduce #(or %1 %2)))
        path (partial path :inventory-pool-groups
                      {:inventory-pool-id @inventory-pool/id*})]
    [:td {:key :groups-roles}
     [roles-component
      (:groups_roles user)
      :compact true]
     [:a.btn.btn-outline-primary.btn-sm.py-0
          {:href (path {:including-user (or (-> user :email presence) (:id user))
                        :role ""})}
          [:span icons/view " " icons/edit " Manage " ]]]))


;### suspended ################################################################

(defn suspension-th-component []
  [:th " Suspension "])

(defn suspension-td-component [user]
  [:td.suspension
   (suspension/suspension-component
     (:suspension user)
     :compact true
     :update-handler (fn [updated]
                       (go (let [data (<! (suspension/put-suspension<
                                            (path :inventory-pool-user-suspension
                                                  {:inventory-pool-id @inventory-pool/id*
                                                   :user-id (:id user)})
                                            updated))]
                             (swap! users/data* assoc-in
                                    [(:url @routing/state*) :users
                                     (:page-index user) :suspension] data)))))])

;### filter ###################################################################

(defn form-role-filter []
  [routing/select-component
   :label "Role"
   :query-params-key :role
   :default-option "customer"
   :options (merge {"" "(any role or none)"
                    "none" "none"}
                   (->> roles/hierarchy
                        (map (fn [%1] [%1 %1]))
                        (into {})))])

(defn form-suspension-filter []
  [routing/select-component
   :label "Suspension"
   :query-params-key :suspension
   :options {"" "(suspended or not)"
             "suspended" "suspended"
             "unsuspended" "unsuspended"}])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [users/form-term-filter]
     [users/form-enabled-filter]
     [form-role-filter]
     [form-suspension-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@inventory-pool-users-count*"
      [:pre (with-out-str (pprint @inventory-pool-users-count*))]]
     [:div "@current-query-params*"
      [:pre (with-out-str (pprint @current-query-params*))]]
     ]))

(defn table-component []
  [users/table-component
   [user-th-component
    roles-th-component
    direct-roles-th-component
    groups-roles-th-component
    suspension-th-component]
   [user-td-component
    roles-td-component
    direct-roles-td-component
    groups-roles-td-component
    suspension-td-component]])

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [table-component]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.inventory-pool-users
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch users/fetch-users))}]
   (breadcrumbs/nav-component
     @breadcrumbs/left*
     [[breadcrumbs/create-li]])
   [:div
    [:h1
     [:span "Users "
      [:span " in the inventory-pool "]
      [inventory-pool/name-link-component]]]
    [main-page-component]]])
