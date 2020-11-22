(ns leihs.admin.resources.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]

    [leihs.admin.defaults :as defaults]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.components :as components]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.utils.seq :as seq]
    [leihs.admin.resources.users.shared :as shared]
    [leihs.admin.resources.users.user.core :as user2]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters*
  (reaction (-> @routing/state* :query-params
                (assoc :term (-> @routing/state* :query-params-raw :term)))))

(def current-url* (reaction (:url @routing/state*)))

(def current-query-paramerters-normalized*
  (reaction (shared/normalized-query-parameters @current-query-paramerters*)))

(def fetch-users-id* (reagent/atom nil))

(def data* (reagent/atom {}))

(defn fetch-users []
  "Fetches the the currernt url with accept/json
  after 1/5 second timeout if query-params have not changed in the meanwhile
  yet and stores the result in the map data* under this url."
  (let [url @current-url*
        normalized-query-params @current-query-paramerters-normalized*]
    (go (<! (timeout 200))
        (when (= url @current-url*)
          (let [resp-chan (async/chan)
                id (requests/send-off {:url url
                                       :method :get}
                                      {:modal false
                                       :title "Fetch Users"
                                       :handler-key :users
                                       :retry-fn #'fetch-users}
                                      :chan resp-chan)]
            (reset! fetch-users-id* id)
            (go (let [resp (<! resp-chan)]
                  (when (and (= (:status resp) 200) ;success
                             (= id @fetch-users-id*) ;still the most recent request
                             (= url @current-url*)) ;query-params have still not changed yet
                    (let [body (-> resp :body)
                          page (:page normalized-query-params)
                          per-page (:per-page normalized-query-params)
                          offset (* per-page (- page 1))]
                      (swap! data* assoc url
                             (-> body
                                 (update-in [:users] (partial seq/with-index offset))
                                 (update-in [:users] (partial seq/with-key :id))
                                 (update-in [:users] (partial seq/with-page-index))
                                 (update-in [:users] #(into [] %)))))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-users)
  (swap! state/global-state*
         assoc :users-query-params @current-query-paramerters-normalized*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @routing/state*)
        (:route-params @routing/state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [routing/form-term-filter-component
   :placeholder "fuzzy term or exact email-address" ])

(defn form-enabled-filter []
  (let [enabled (or (some-> @current-query-paramerters-normalized* :account_enabled)
                    "")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-enabled} "Account enabled"]
     [:select#users-filter-enabled.form-control
      {:value enabled
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :account_enabled val}))))}
      (for [[k n] {"any" "any"
                   "yes" "yes"
                   "no" "no"}]
        [:option {:key k :value k} n])]]))


(defn form-admins-filter []
  (let [is-admin (or (-> @current-query-paramerters-normalized* :is_admin)
                     "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label {:for :users-filter-is-admin} [:span.pr-1 "Is admin"]]
     [:select#users-filter-is-admin.form-control
      {:value is-admin
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :is_admin val}))))}
      (for [[k n] {"any" "any"
                   "yes" "yes"
                   "no" "no"}]
        [:option {:key k :value k} n])]]))

(defn form-org-filter []
  (let [org-id (some-> @current-query-paramerters-normalized* :org_id)]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-org-id} "Org id"]
     [:input#users-filter-org-id.form-control
      {:type :text
       :value org-id
       :placeholder "org_id or true or false"
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :org_id val}))))}]]))

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [form-term-filter]
    [form-org-filter]
    [form-enabled-filter]
    [form-admins-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component :default-query-params shared/default-query-params]]]])

;;; user

(defn user-th-component []
  [:th "User"])

(defn user-td-inner-component [user]
  [:ul.list-unstyled
   (for [[idx item] (map-indexed vector (user2/fullname-some-id-seq user))]
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
  [:th {:key :protected} "Protected"])

(defn protected-td-component [group]
  [:td {:key :protected}
   (if (:protected group)
     "yes"
     "no")])


;;; org_id

(defn org-id-th-component []
  [:th "Org_id"])

(defn org-id-td-component [user]
  [:td (:org_id user) ])

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
    [protected-th-component]
    [org-id-th-component]
    [:th {:key :image} "Image"]
    (for [[idx hd] (map-indexed vector hds)]
      ^{:key idx} [hd])]])


(defn row-component [user more-cols]
  [:tr.user {:key (:id user)}
   [:td (:index user)]
   ^{:key :protected} [protected-td-component user]
   ^{:key :org-id} [org-id-td-component user]
   [:td [components/img-small-component user]]
   (for [[idx col] (map-indexed vector more-cols)]
     ^{:key idx} [col user])])

(defn table-component [hds tds]
  (if-not (contains? @data* @current-url*)
    [wait-component]
    (if-let [users (-> @data* (get  @current-url* {}) :users seq)]
      [:table.table.table-striped.table-sm.users
       [thead-component hds]
       [:tbody.users
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [user users]
                   (row-component user tds))))]]
      [:div.alert.alert-warning.text-center "No (more) users found."])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Users Debug"]
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
    {:did-mount escalate-query-paramas-update
     :did-update escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [table-component
    [user-th-component
     account-enabled-th-component
     contracts-count-th-component
     pools-count-th-component
     groups-count-th-component]
    [user-td-component
     account-enabled-td-component
     org-id-td-component
     contracts-count-td-component
     pools-count-td-component
     groups-count-td-component]]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.users
   [breadcrumbs/nav-component
     [[breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/users-li]]
     [[breadcrumbs/user-create-li]]]
   [:h1 "Users"]
   [main-page-content-component]])