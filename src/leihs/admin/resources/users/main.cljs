(ns leihs.admin.resources.users.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.common.membership.users.shared :as users-membership]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.users.shared :as shared]
   [leihs.admin.resources.users.user.core :as user]
   [leihs.admin.resources.users.user.create :as create]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Alert]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-params*
  (reaction (merge shared/default-query-params
                   (:query-params-raw @routing/state*))))

(def current-route* (reaction (:route @routing/state*)))

(def data* (reagent/atom {}))

(defn fetch-users []
  (http/route-cached-fetch data*))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def on-first-page?*
  (reaction (= 1 (get-in @routing/state* [:query-params :page] 1))))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [filter/form-term-filter-component
   :placeholder "part of the name, exact email-address"])

(defn form-enabled-filter []
  [filter/select-component
   :query-params-key :account_enabled
   :label "Enabled"
   :options {"" "(any value)" "yes" "yes" "no" "no"}
   :default-option "yes"])

(defn form-admins-filter []
  [filter/select-component
   :label "Admin"
   :query-params-key :admin
   :options {"" "(any value)"
             "leihs-admin" "Leihs admin"
             "system-admin" "System admin"}])

;;; filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [filter/container
   [:<>
    [form-term-filter]
    [form-enabled-filter]
    [users-and-groups/form-org-filter data*]
    [users-and-groups/form-org-id-filter]
    [form-admins-filter]
    [users-and-groups/protected-filter]
    [filter/form-per-page]
    [filter/reset :default-query-params shared/default-query-params]]])

;;; user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-th-component []
  [:th "User"])

(defn user-td-inner-component [user]
  [:ul.list-unstyled
   (for [[idx item] (map-indexed vector (user/fullname-some-uid-seq user))]
     ^{key idx} [:li {:key idx} item])])

(defn user-td-component [user]
  [:td [:a {:href (path :user {:user-id (:id user)})}
        [user-td-inner-component user]]])

;;; account enabled

(defn account-enabled-th-component []
  [:th "Enabled"])

(defn account-enabled-td-component [user]
  [:td (if (:account_enabled user)
         [:span.text-success "yes"]
         [:span.text-warning "no"])])

;;; org_id

(defn org-id-th-component []
  [:th "Org_id"])

(defn org-id-td-component [user]
  [:td (:org_id user)])

;;; org

(defn org-th-component []
  [:th {:key :organization} "Organization"])

(defn org-td-component [group]
  [:td {:key :organization}
   (:organization group)])

;;; counts

(defn contracts-count-th-component []
  [:th.text-right "# Contracts"])

(defn contracts-count-td-component [user]
  [:td.text-right (str (:open_contracts_count user)
                       "/" (:closed_contracts_count user))])

(defn pools-count-th-component []
  [:th.text-right "# Pools"])

(defn pools-count-td-component [user]
  [:td.text-right (:pools_count user)])

(defn groups-count-th-component []
  [:th.text-right "# Groups"])

(defn groups-count-td-component [user]
  [:td.text-right (:groups_count user)])

;; table stuff ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-head [hds]
  [:tr
   [:th {:key :index} "Index"]
   [account-enabled-th-component]
   [:th {:key :image} "Image"]
   (for [[idx hd] (map-indexed vector hds)]
     ^{:key idx} [hd])])

(defn table-row [user more-cols]
  [:tr.user {:key (:id user)}
   [:td (:index user)]
   [account-enabled-td-component user]
   [:td [components/img-small-component user]]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col user])])

(defn add-button []
  (let [show (reagent/atom false)]
    (reset! user/user-data* {})
    (fn []
      (when (auth/allowed?
             [auth/admin-scopes? pool-auth/some-lending-manager?])
        [:<>
         [:> Button
          {:className "ml-3"
           :onClick #(reset! show true)}
          "Add User"]
         [create/dialog {:show @show
                         :onHide #(reset! show false)}]]))))

(defn users-table
  [hds tds &
   {:keys [membership-filter? role-filter?]
    :or {membership-filter? false
         role-filter? false}}]
  [:<>
   [routing/hidden-state-component
    {:did-change fetch-users}]
   (if-not (contains? @data* @current-route*)
     [wait-component]
     (if-let [users (-> @data* (get  @current-route* {}) :users seq)]
       [table/container
        {:className "users"
         :header (table-head hds)
         :body (doall (for [user users]
                        (table-row user tds)))}]
       (if @on-first-page?*
         (cond
           (and membership-filter? @users-membership/filtered-by-member?*) (users-membership/empty-members-alert)
           (and role-filter? @roles/filtered-by-role?*) (roles/empty-alert)
           :else [:> Alert {:variant "info" :className "my-3 text-center"} "No users found."])
         [:> Alert {:variant "info" :className "my-3 text-center"} "No more users found."])))])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Users Debug"]
     [:div
      [:h3 "@current-query-params*"]
      [:pre (with-out-str (pprint @current-query-params*))]]
     [:div
      [:h3 "@current-route*"]
      [:pre (with-out-str (pprint @current-route*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:article.users.my-5
   [:h1.my-5
    [icons/users] " Users"]
   [:section
    [filter-component]
    [table/toolbar  [add-button]]
    [users-table
     [user-th-component
      org-th-component
      org-id-th-component
      contracts-count-th-component
      pools-count-th-component
      groups-count-th-component]
     [user-td-component
      org-td-component
      org-id-td-component
      contracts-count-td-component
      pools-count-td-component
      groups-count-td-component]]
    [table/toolbar  [add-button]]
    [debug-component]]])
