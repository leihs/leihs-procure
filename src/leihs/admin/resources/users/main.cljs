(ns leihs.admin.resources.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :as str]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.common.users-and-groups.core :as users-and-groups]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.users.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.shared :as shared]
    [leihs.admin.resources.users.user.core :as user]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [reagent.core :as reagent]
    ))

(def current-query-params*
  (reaction (merge shared/default-query-params
                   (:query-params-raw @routing/state*)
                   )))

(def current-route* (reaction (:route @routing/state*)))


(def data* (reagent/atom {}))

(defn fetch-users []
  (http/route-cached-fetch data*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-params*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [routing/form-term-filter-component
   :placeholder "part of the name, exact email-address" ])

(defn form-enabled-filter []
  [routing/select-component
   :query-params-key :account_enabled
   :label "Enabled"
   :options {"" "(any value)" "yes" "yes" "no" "no"}
   :default-option "yes"])

(defn form-admins-filter []
  [routing/select-component
   :label "Admin"
   :query-params-key :admin
   :options {"" "(any value)"
             "leihs-admin" "Leihs admin"
             "system-admin" "System admin"}])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [form-term-filter]
     [form-enabled-filter]
     [users-and-groups/form-org-filter data*]
     [users-and-groups/form-org-id-filter]
     [form-admins-filter]
     [users-and-groups/protected-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component :default-query-params shared/default-query-params]]]])

;;; user

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


;;; protected

(defn protected-th-component []
  [:th {:key :admin_protected} "Protected"])

(defn protected-td-component [group]
  [:td {:key :admin_protected}
   (if (:admin_protected group)
     "yes"
     "no")])


;;; org_id

(defn org-id-th-component []
  [:th "Org_id"])

(defn org-id-td-component [user]
  [:td (:org_id user) ])

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

(defn thead-component [hds]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    [account-enabled-th-component]
    [:th {:key :image} "Image"]
    (for [[idx hd] (map-indexed vector hds)]
      ^{:key idx} [hd])]])


(defn row-component [user more-cols]
  [:tr.user {:key (:id user)}
   [:td (:index user)]
   [account-enabled-td-component user]
   [:td [components/img-small-component user]]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col user])])

(defn table-component [hds tds]
  [:div
   [routing/hidden-state-component
    {:did-change fetch-users}]
   (if-not (contains? @data* @current-route*)
     [wait-component]
     (if-let [users (-> @data* (get  @current-route* {}) :users seq)]
       [:table.table.table-striped.table-sm.users
        [thead-component hds]
        [:tbody.users
         (doall (for [user users]
                  (row-component user tds)))]]
       [:div.alert.alert-warning.text-center "No (more) users found."]))])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn main-page-content-component []
  [:div
   [filter-component]
   [routing/pagination-component]
   [table-component
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
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.users
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/user-create-li]]]
   [:h1 [icons/users] " Users"]
   [main-page-content-component]])
