(ns leihs.admin.resources.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [leihs.admin.utils.seq :refer [with-index]]
    [leihs.admin.resources.users.shared :as shared]

    [clojure.string :as str]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def current-query-paramerters* (reaction (-> @state/routing-state* :query-params)))

(def current-url* (reaction (:url @state/routing-state*)))

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
                          offset (* per-page (- page 1))
                          body-with-indexed-users (update-in body [:users] (partial with-index offset))]
                      (swap! data* assoc url body-with-indexed-users))))))))))

(defn escalate-query-paramas-update [_]
  (fetch-users)
  (swap! state/global-state*
         assoc :users-query-params @current-query-paramerters-normalized*))


;;; helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-path-for-query-params [query-params]
  (path (:handler-key @state/routing-state*) 
        (:route-params @state/routing-state*)
        (merge @current-query-paramerters-normalized*
               query-params)))


;;; Filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-term-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.sr-only {:for :users-search-term} "Search term"]
   [:input#users-search-term.form-control.mb-1.mr-sm-1.mb-sm-0
    {:type :text
     :placeholder "Search term ..."
     :value (or (-> @current-query-paramerters-normalized* :term presence) "")
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (page-path-for-query-params
                                            {:page 1 :term val}))))}]])

(defn form-admins-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Admins only"]
    [:input
     {:type :checkbox
      :on-change #(let [new-state (case (-> @current-query-paramerters-normalized*
                                            :is_admin presence)
                                    ("true" true) nil
                                    true)]
                    (js/console.log (with-out-str (pprint new-state)))
                    (accountant/navigate! (page-path-for-query-params 
                                             {:page 1
                                              :is_admin new-state})))
      :checked (case (-> @current-query-paramerters-normalized*
                         :is_admin presence)
                 (nil false "false") false
                 ("true" true) true)}]]])

(defn form-type-filter []
  (let [type (or (-> @current-query-paramerters-normalized* :type presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-type} "Type"]
     [:select#users-filter-type.form-control
      {:value type
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params 
                                              {:page 1
                                               :type val}))))}
      (for [t ["any" "org" "manual"]]
        [:option {:key t :value t} t])]]))

(defn form-role-filter []
  (let [role (or (-> @current-query-paramerters-normalized* :role presence) "any")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-role} "Role"]
     [:select#users-filter-role.form-control
      {:value role
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "")]
                      (accountant/navigate! (page-path-for-query-params 
                                              {:page 1
                                               :role val}))))}
      (for [a [ "any" "customer" "group_manager" "inventory_manager" "lending_manager"]]
        [:option {:key a :value a} a])]]))

(defn form-per-page []
  (let [per-page (or (-> @current-query-paramerters-normalized* :per-page presence) "12")]
    [:div.form-group.ml-2.mr-2.mt-2
     [:label.mr-1 {:for :users-filter-per-page} "Per page"]
     [:select#users-filter-per-page.form-control
      {:value per-page
       :on-change (fn [e]
                    (let [val (or (-> e .-target .-value presence) "12")]
                      (accountant/navigate! (page-path-for-query-params
                                              {:page 1
                                               :per-page val}))))}
      (for [p [12 25 50 100 250 500 1000]]
        [:option {:key p :value p} p])]]))

(defn form-reset []
  [:div.form-group.mt-2
   [:label.sr-only {:for :users-filter-reset} "Reset"]
   [:a#users-filter-reset.btn.btn-warning
    {:href (page-path-for-query-params shared/default-query-parameters)}
    [:i.fas.fa-times]
    " Reset "]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [form-term-filter]
    [form-admins-filter]
    [form-role-filter]
    [form-type-filter]
    [form-per-page]
    [form-reset]]]])

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
    (for [{th :th key :key} (:customcols colconfig)]
      [th {:key key}])]])

(defn user-row-component [colconfig user]
  [:tr {:key (:id user)}
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
            [:span.firstname (-> user :firstname str/trim presence)]
            " "
            [:span.lastname (-> user :lastname str/trim presence)]]]])
   (when (:email colconfig)
     [:td [:a {:href (str "mailto:" (:email user))}
           [:i.fas.fa-envelope] " " (:email user)]])
   (for [{td :td} (:customcols colconfig)]
     [td user])])
 
(defn users-table-component [colconfig]
  (if-not (contains? @data* @current-url*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]
     [:span.sr-only "Please wait"]]
    (if-let [users (-> @data* (get  @current-url* {}) :users seq)]
      [:table.table.table-striped.table-sm
       [users-thead-component colconfig]
       [:tbody
        (let [page (:page @current-query-paramerters-normalized*)
              per-page (:per-page @current-query-paramerters-normalized*)]
          (doall (for [user users]
                   (user-row-component colconfig user))))]]
      [:div.alert.alert-warning.text-center "No (more) users found."])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pagination-component []
  [:div.clearfix.mt-2.mb-2
   (let [page (dec (:page @current-query-paramerters-normalized*))]
     [:div.float-left
      [:a.btn.btn-primary.btn-sm
       {:class (when (< page 1) "disabled")
        :href (page-path-for-query-params {:page page})}
       [:i.fas.fa-arrow-circle-left] " Previous " ]])
   [:div.float-right
    [:a.btn.btn-primary.btn-sm
     {:href (page-path-for-query-params 
              {:page (inc (:page @current-query-paramerters-normalized*))})}
     " Next " [:i.fas.fa-arrow-circle-right]]]])

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

(defn main-page-content-component [colconfig]
  [:div
   [state/hidden-routing-state-component
    {:will-mount escalate-query-paramas-update
     :did-update escalate-query-paramas-update}]
   [filter-component]
   [pagination-component]
   [users-table-component colconfig]
   [pagination-component]
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
