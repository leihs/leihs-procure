(ns leihs.admin.resources.groups.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.common.users-and-groups.core :as users-and-groups]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.groups.shared :as shared]
    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.utils.seq :as seq]
    [leihs.core.auth.core :as auth :refer []]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as current-user]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:route @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def data* (reagent/atom {}))

(defn fetch-groups []
  (http/route-cached-fetch data*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))

(defn link-to-group
  [group inner & {:keys [authorizers]
                  :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :group {:group-id (:id group)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn form-term-filter []
  [routing/form-term-filter-component
   :placeholder "name of the group"])

(defn form-including-user-filter []
  [routing/choose-user-component
   :query-params-key :including-user
   :input-options {:placeholder "email, login, or id"}])

(defn form-org-filter []
  [routing/delayed-query-params-input-component
   :label "Org ID"
   :query-params-key :org_id
   :input-options
   {:placeholder "org_id or true or false"}])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [form-term-filter]
     [form-including-user-filter]
     [users-and-groups/form-org-filter data*]
     [users-and-groups/form-org-id-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [group]
  [:td {:key :name}
   [link-to-group group [:span (:name group) ]
    :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]]])

(defn org-th-component []
 [:th {:key :organization} "Organization"])

(defn org-td-component [group]
  [:td {:key :organization}
   (:organization group)])

(defn org-id-th-component []
 [:th {:key :org-id} "Org ID"])

(defn org-id-td-component [group]
  [:td {:key :org-id}
   (:org_id group)])

(defn protected-th-component []
  [:th {:key :admin_protected} "Protected"])

(defn protected-td-component [group]
  [:td {:key :admin_protected}
   (if (:admin_protected group)
     "yes"
     "no")])

(defn users-count-th-component []
  [:th.text-right {:key :count_users} "# Users"])

(defn users-count-td-component [group]
  [:td.text-right {:key :users_count} (:count_users group)])


;;;;;

(defn groups-thead-component [more-cols]
  [:thead
   [:tr
    [:th {:key :index} "Index"]
    (for [[idx col] (map-indexed vector more-cols)]
      ^{:key idx} [col])]])

(defn group-row-component [group more-cols]
  ^{:key (:id group)}
  [:tr.group {:key (:id group)}
   [:td {:key :index} (:index group)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col group])])

(defn core-table-component [hds tds groups]
  (if-let [groups (seq groups)]
    [:table.groups.table.table-striped.table-sm
     [groups-thead-component hds]
     [:tbody
      (let [page (:page @current-query-paramerters-normalized*)
            per-page (:per-page @current-query-paramerters-normalized*)]
        (doall (for [group groups]
                 ^{:key (:id group)}
                 [group-row-component group tds])))]]
    [:div.alert.alert-warning.text-center "No (more) groups found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* (:route @routing/state*))
    [wait-component]
    [core-table-component hds tds
     (-> @data* (get (:route @routing/state*) {}) :groups)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-paramerters-normalized*"]
      [:pre (with-out-str (pprint @current-query-paramerters-normalized*))]]
     [:div
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn main-page-content-component []
  [:div
   [routing/hidden-state-component
    {:did-change fetch-groups}]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [name-th-component
     org-th-component
     org-id-th-component
     users-count-th-component]
    [name-td-component
     org-td-component
     org-id-td-component
     users-count-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.groups
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/create-li]]]
   [:h1 [icons/groups] " Groups"]
   [main-page-content-component]])
