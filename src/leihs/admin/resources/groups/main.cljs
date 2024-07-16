(ns leihs.admin.resources.groups.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.common.users-and-groups.core :as users-and-groups]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.create :as create]
   [leihs.admin.resources.groups.shared :as shared]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [fetch-route* wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert]]
   [reagent.core :as reagent :refer [reaction]]))

(def current-query-parameters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-query-parameters-normalized*
  (reaction (shared/normalized-query-parameters
             @current-query-parameters*)))

(def current-url*
  (reaction
   (path (:handler-key @routing/state*)
         (:route-params @routing/state*))))

(def data* (reagent/atom nil))

(defn fetch-groups []
  (http/route-cached-fetch data* {:route @fetch-route*}))

;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-parameters-normalized*
               query-params)))

(defn link-to-group
  [group inner & {:keys [authorizers]
                  :or {authorizers []}}]
  (if (auth/allowed? authorizers)
    [:a {:href (path :group {:group-id (:id group)})} inner]
    inner))

;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [filter/form-term-filter-component :placeholder "Name of the Group"])

(defn form-including-user-filter []
  [filter/choose-user-component
   :query-params-key :including-user
   :input-options {:placeholder "email, login, or id"}])

(defn filter-component []
  [filter/container
   [:<>
    [form-term-filter]
    [filter/form-including-user]
    [users-and-groups/form-org-filter data*]
    [users-and-groups/form-org-id-filter]
    [filter/form-per-page]
    [filter/reset]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-th-component []
  [:th {:key :name} "Name"])

(defn name-td-component [group]
  [:td {:key :name}
   [link-to-group group [:span (:name group)]
    :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]]])

(defn name-td-component-no-link [group]
  [:td {:key :name}
   (:name group)])

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

(defn users-count-th-component []
  [:th.text-right {:key :count_users} "# Users"])

(defn users-count-td-component [group]
  [:td.text-right {:key :users_count} (:count_users group)])

;;;;;

(defn groups-thead-component [more-cols]
  [:tr
   [:th {:key :index} "Index"]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col])])

(defn group-row-component [group more-cols]
  ^{:key (:id group)}
  [:tr.group {:key (:id group)}
   [:td {:key :index} (:index group)]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col group])])

(defn core-table-component [hds tds groups]
  (if-let [groups (seq groups)]
    [:<>
     [table/container
      {:className "groups"
       :header (groups-thead-component hds)
       :body (doall (for [group groups]
                      ^{:key (:id group)}
                      [group-row-component group tds]))}]]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No (more) groups found."]))

(defn table-component [hds tds]
  (if-not (contains? @data* @fetch-route*)
    [wait-component]
    [core-table-component hds tds (-> @data*
                                      (get @fetch-route*)
                                      :groups seq)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@current-query-parameters-normalized*"]
      [:pre (with-out-str (pprint @current-query-parameters-normalized*))]]
     [:div
      [:h3 "@current-url*"]
      [:pre (with-out-str (pprint @current-url*))]]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-change #(fetch-groups)}]

   [:article.groups.my-5
    [:header.my-5
     [:h1
      [icons/groups] " Groups"]]

    [:section.mb-5
     [filter-component]
     [table/toolbar [create/button]]
     [table-component
      [name-th-component
       org-th-component
       org-id-th-component
       users-count-th-component]
      [name-td-component
       org-td-component
       org-id-td-component
       users-count-td-component]]
     [table/toolbar [create/button]]
     [create/dialog]]

    [debug-component]]])
