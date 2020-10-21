(ns leihs.admin.resources.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]

    [leihs.admin.defaults :as defaults]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :as front-shared :refer [gravatar-url wait-component]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.core.routing.front :as routing]

    [leihs.admin.utils.seq :as seq]
    [leihs.admin.resources.users.shared :as shared]

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
  [:div.form-group.ml-2.mr-2.mt-2
   [:label {:for :users-search-term} "Fuzzy and email search"]
   [:input#users-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term or e-mail ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :term val}))))}]])


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
  (let [is-admin (-> @current-query-paramerters-normalized* :is_admin)]
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
    [routing/form-reset-component]]]])

;;; Table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-colconfig
  {:id true
   :org_id true
   :name true
   :email true
   :customcols []})

(defn user-link-component [user inner-component]
  [:a {:href (path :user {:user-id (:id user)})}
   inner-component])

(defn users-thead-component [colconfig]
  [:thead
   [:tr
    [:th "Index"]
    [:th "Image" ]
    (when (:org_id colconfig) [:th "Org id"])
    (when (:name colconfig) [:th "Name"])
    (when (:email colconfig) [:th "Email"])
    (doall
      (for [{th :th key :key} (:customcols colconfig)]
        [th {:key key}]))]])

(defn user-row-component [colconfig user]
  [:tr.user {:key (:id user)}
   [:td (user-link-component user (:index user))]
   [:td [:a {:href (path :user {:user-id (:id user)})}
         [:img
          {:height 32
           :width 32
           :src (or (:img32_url user)
                    (gravatar-url (:email user)))}]]]
   (when (:org_id colconfig)
     [:td [user-link-component user
           [:span {:style {:font-family "monospace"}}
            (:org_id user)]]])
   (when (:name colconfig)
     [:td [user-link-component user
           [:span
            [:span.firstname (some-> user :firstname str/trim presence)]
            " "
            [:span.lastname (some-> user :lastname str/trim presence)]]]])
   (when (:email colconfig)
     [:td [:a {:href (str "mailto:" (:email user))}
           [:i.fas.fa-envelope] " " (:email user)]])
   (for [[idx {td :td}] (map-indexed vector (:customcols colconfig))]
     ^{:key idx} [td user])])

(defn users-table-component [colconfig]
  (if-not (contains? @data* @current-url*)
    [wait-component]
    (if-let [users (-> @data* (get  @current-url* {}) :users seq)]
      [:table.table.table-striped.table-sm.users
       [users-thead-component colconfig]
       [:tbody.users
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [user users]
                   (user-row-component colconfig user))))]]
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

(defn main-page-content-component [colconfig]
  [:div
   [routing/hidden-state-component
    {:did-mount escalate-query-paramas-update
     :did-update escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [users-table-component colconfig]
   [routing/pagination-component]
   [debug-component]])

(defn page []
  [:div.users
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)]
     [(breadcrumbs/user-add-li)])
   [:h1 "Users"]
   [main-page-content-component default-colconfig]])
