(ns leihs.admin.resources.inventory-pools.inventory-pool.users.front
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
    [leihs.core.icons :as icons]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy roles-component]]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.suspension.front :as suspension]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.regex :as regex]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(def inventory-pool-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :inventory-pool_users_count)))

;### roles ####################################################################

(defn roles-th-component []
  [:th {:key :roles} " Roles "])

(defn roles-td-component [user]
  (let [path (path :inventory-pool-user
                   {:inventory-pool-id @inventory-pool-id* :user-id (:id user)})]
    [:td {:key :roles}
     [roles-component user {}]
     [:a.btn.btn-outline-primary.btn-sm {:href path} [:i.fas.fa-eye] " details"]]))



;### direct roles #############################################################

(defn direct-roles-th-component []
  [:th {:key :direct-roles} " Direct roles "])

(defn remove-direct-roles [user]
  (let  [path (path :inventory-pool-user-direct-roles
                                    {:user-id (:id user)
                                     :inventory-pool-id @inventory-pool-id*})]
    (let [resp-chan (async/chan)
          id (requests/send-off {:url path
                                 :method :delete }
                                {:modal true
                                 :title "Remove direct roles"
                                 :retry-fn #'remove-direct-roles}
                                :chan resp-chan)]
      (go (let [resp (<! resp-chan)]
            (users/fetch-users))))))

(defn direct-roles-td-component [user]
  (let [has-a-role? (some->> user :direct_roles vals (reduce #(or %1 %2)))
        path (path :inventory-pool-user-direct-roles
                   {:inventory-pool-id @inventory-pool-id* :user-id (:id user)})]
    [:td.direct-roles {:key :direct-roles}
     [roles-component user {:ks [:direct_roles]}]
     (if has-a-role?
       [:span
        [:a.btn.btn-outline-primary.btn-sm.m-1 {:href path}
         [:span icons/edit " edit " ]]
        [:button.btn.btn-warning.btn-sm.m-1
         {:on-click #(remove-direct-roles user)}
         [:span icons/delete " remove "] ]]
       [:a.btn.btn-outline-primary.btn-sm.m-1
        {:href path}
        [:span icons/add " add " ]])]))



;### groups roles #############################################################


(defn groups-roles-th-component []
  [:th {:key :groups-roles} " Roles via groups "])

(defn groups-roles-td-component [user]
  (let [has-a-role? (some->> user :groups_roles vals (reduce #(or %1 %2)))
        path (path :inventory-pool-groups
                   {:inventory-pool-id @inventory-pool-id*}
                   {:including-user (or (-> user :email presence) (:id user))})]
    [:td {:key :groups-roles}
     [roles-component user {:ks [:groups_roles]}]
     (if has-a-role?
       [:span
        [:a.btn.btn-outline-primary.btn-sm.m-1 {:href path}
         [:span icons/view " details " " / " icons/edit " edit"]]]
       [:a.btn.btn-outline-primary.btn-sm.m-1
        {:href path}
        [:span icons/add " add " ]])]))


;### suspended ################################################################

(defn suspension-th-component []
  [:th " Suspension "])



(defn suspension-td-component [user]
  (let [user-suspension-path (path :inventory-pool-user-suspension
                                   {:user-id (:id user)
                                    :inventory-pool-id @inventory-pool-id*})
        user-inventory-pool-path (path :inventory-pool-user
                                       {:inventory-pool-id @inventory-pool-id*
                                        :user-id (:id user)})]
  [:td.suspension
   (if-let [suspended_until (some-> user :suspended_until js/moment)]
     [:div
      [:div
       [:span.text-danger.m-1
        [:span
         (if (.isAfter suspended_until "2098-01-01")
           "forever"
           (.format suspended_until "YYYY-MM-DD"))]]]
      [:span
       [:span
        [:a.btn.btn-outline-primary.btn.btn-sm.m-1
         {:href user-inventory-pool-path}
           icons/view "  details" ]
        [:a.btn.btn-outline-primary.btn.btn-sm.m-1
         {:href user-suspension-path}
         icons/edit "edit " ]
        [:button.btn.btn-warning.btn.btn-sm.m-1
         {:on-click #(suspension/cancel user users/fetch-users)}
         icons/delete " cancel "]]
       ]]
     [:div
      [:div.m-1
       [:span.text-success "unsuspended" ]]
      [:div.m-1
       [:a.btn.btn-outline-primary.btn-sm
        {:href user-suspension-path }
        icons/edit " suspend" ]]])]))


;### actions ##################################################################

(def colconfig
  (merge users/default-colconfig
         {:email false
          :org_id false
          :customcols [
                       {:key :roles
                        :th roles-th-component
                        :td roles-td-component}
                       {:key :direct-roles
                        :th direct-roles-th-component
                        :td direct-roles-td-component}
                       {:key :groups-roles
                        :th groups-roles-th-component
                        :td groups-roles-td-component}
                       {:key :suspension
                        :th suspension-th-component
                        :td suspension-td-component}]}))


;### filter ###################################################################

(defn form-role-filter []
  (let [role (or (-> @users/current-query-paramerters-normalized* :role presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-role} " Role "]
     [:select#users-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (users/page-path-for-query-params
                                              {:page 1
                                               :role val}))))}
      (doall (for [a (concat ["any" "none"] roles-hierarchy)]
               [:option {:key a :value a} a]))]]))


(defn form-suspension-filter []
  (let [suspended (or (:suspension @users/current-query-paramerters-normalized*)
                      "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-suspended-filter} " Suspension "]
     [:select#users-suspended-filter.form-control
      {:value suspended
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (users/page-path-for-query-params
                                              {:page 1
                                               :suspension val}))))}
      (doall (for [[n k] {"any" ""
                          "suspended" "suspended"
                          "unsuspended" "unsuspended" }]
               [:option {:key k :value k} n]))]]))



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
      [:pre (with-out-str (pprint @inventory-pool-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [users/users-table-component colconfig]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.inventory-pool-users
   [routing/hidden-state-component
    {:did-mount (fn [_] (inventory-pool/clean-and-fetch users/fetch-users))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*)
      (breadcrumbs/inventory-pool-users-li @inventory-pool/inventory-pool-id*)]
     [])
   [:div
    [:h1
     [:span "Users "
      [:span " in the inventory-pool "]
      [:a {:href (path :inventory-pool {:inventory-pool-id @inventory-pool-id*})}
       [inventory-pool/name-component]]]]
    [main-page-component]]])
