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
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy roles-component]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles-ui :as roles-ui]
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
  [:td {:key :roles} [roles-component user {}]])


;### direct roles #############################################################

(defn direct-roles-th-component []
  [:th {:key :direct-roles} " Direct roles "])

(defn update-direct-roles [user roles success-chan]
  (let  [path (path :inventory-pool-user-direct-roles
                    {:user-id (:id user)
                     :inventory-pool-id @inventory-pool/id*})]
    (let [resp-chan (async/chan)
          id (requests/send-off {:url path
                                 :method :put
                                 :json-params {:roles roles}}
                                {:modal true
                                 :title "Update direct roles"
                                 :retry-fn #'update-direct-roles}
                                :chan resp-chan)]
      (go (let [resp (<! resp-chan)]
            (users/fetch-users)
            (async/put! success-chan roles))))))

(defn direct-roles-td-component [user]
  [:td.direct-roles {:key :direct-roles}
   [roles-ui/inline-roles-form-component
    (get user :direct_roles)
    (fn [roles chan] (update-direct-roles user roles chan))]])


;### groups roles #############################################################

(defn groups-roles-th-component []
  [:th {:key :groups-roles} " Roles via groups "])

(defn groups-roles-td-component [user]
  (let [has-a-role? (some->> user :groups_roles vals (reduce #(or %1 %2)))
        path (partial path :inventory-pool-groups
                      {:inventory-pool-id @inventory-pool/id*})]
    [:td {:key :groups-roles}
     [roles-component user {:ks [:groups_roles]}]
     (if has-a-role?
       [:span
        [:a.btn.btn-outline-primary.btn-sm.m-1
         {:href (path {:including-user (or (-> user :email presence) (:id user))})}
         [:span icons/view " details " " / " icons/edit " edit"]]]
       (when (> (:groups_count user) 0)
         [:a.btn.btn-outline-primary.btn-sm.m-1
          {:href (path {:including-user (or (-> user :email presence) (:id user))
                        :role ""})}
          [:span icons/add " Add " ]]))]))


;### suspended ################################################################

(defn suspension-th-component []
  [:th " Suspension "])

(defn suspension-td-component [user]
  (let [user-suspension-path (path :inventory-pool-user-suspension
                                   {:user-id (:id user)
                                    :inventory-pool-id @inventory-pool/id*})
        user-inventory-pool-path (path :inventory-pool-user
                                       {:inventory-pool-id @inventory-pool/id*
                                        :user-id (:id user)})]
    [:td.suspension
     (let [suspended-until (some-> user :suspended_until js/Date.)]
       [:div
        [:div [suspension/humanized-suspended-until-component
               suspended-until]]
        (if suspended-until
          [:span
           [:span
            [:a.btn.btn-outline-primary.btn.btn-sm.m-1
             {:href user-inventory-pool-path}
             icons/view "  Details" ]
            [:a.btn.btn-outline-primary.btn.btn-sm.m-1
             {:href user-suspension-path}
             icons/edit " Edit " ]
            [:button.btn.btn-warning.btn.btn-sm.m-1
             {:on-click #(suspension/cancel user users/fetch-users)}
             icons/delete " Cancel "]]]
          [:div.m-1
           [:a.btn.btn-outline-primary.btn-sm
            {:href user-suspension-path }
            icons/edit " Suspend" ]])])]))


;### filter ###################################################################

(defn form-role-filter []
  [routing/select-component
   :label "Role"
   :query-params-key :role
   :default-option "customer"
   :options (merge {"" "(any role or none)"
                    "none" "none"}
                   (->> roles-hierarchy
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
