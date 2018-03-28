(ns leihs.admin.resources.delegation.front
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
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(declare fetch-delegation fetch-resposible-user responsible-user-data*)
(defonce delegation-id* (reaction (-> @state/routing-state* :route-params :delegation-id)))
(defonce delegation-data* (reagent/atom nil))

(def fetch-delegation-id* (reagent/atom nil))
(defn fetch-delegation []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :delegation (-> @state/routing-state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Delegation"
                               :handler-key :delegation
                               :retry-fn #'fetch-delegation}
                              :chan resp-chan)]
    (reset! fetch-delegation-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-delegation-id*))
            (reset! delegation-data*
                    (merge (:body resp)
                           (:query-params @state/routing-state*)))
            (reset! responsible-user-data* nil)
            (fetch-resposible-user))))))


(defonce edit-mode?*
  (reaction
    (and (map? @delegation-data*)
         (boolean ((set '(:delegation-edit :delegation-add))
                   (:handler-key @state/routing-state*))))))


;;; responsible user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def responsible-user-id*
	(reaction
		(re-matches regex/uuid-pattern
								(or (-> @delegation-data* :responsible_user_id presence)
										""))))

(def responsible-user-data* (reagent/atom nil))

(def fetch-resposible-user-id* (reagent/atom nil))
(defn fetch-resposible-user []
	(when-let [id @responsible-user-id*]
		(let [resp-chan (async/chan)
					id (requests/send-off {:url (path :user {:user-id id})
																 :method :get
																 :query-params {}}
																{:modal false
																 :title "Fetch Responsible User"
																 :handler-key :delegation
																 :retry-fn #'fetch-resposible-user}
																:chan resp-chan)]
			(reset! fetch-resposible-user-id* id)
			(go (let [resp (<! resp-chan)]
						(when (and (= (:status resp) 200)
											 (= id @fetch-resposible-user-id*))
							(reset! responsible-user-data* (:body resp))))))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! delegation-data* nil)
  (fetch-delegation))

(defn fetch [_]
  (fetch-delegation))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text} opts)]
     [:div.form-group.row
      [:label.col.col-form-label.col-sm-2 {:for kw} kw]
      [:div.col
       [:input.form-control
        {:id kw
         :type (:type opts)
         :value (or (kw @delegation-data*) "")
         :on-change #(swap! delegation-data* assoc kw (-> % .-target .-value presence))
         :disabled (not @edit-mode?*)}]]])))

(defn checkbox-component [kw]
  [:div.form-check.form-check-inline
   [:label {:for kw}
    [:input
     {:id kw
      :type :checkbox
      :checked (kw @delegation-data*)
      :on-change #(swap! delegation-data* assoc kw (-> @delegation-data* kw boolean not))
      :disabled (not @edit-mode?*)}]
    [:span.ml-2 kw]]])

(defn debug-component []
	(when (:debug @state/global-state*)
		[:div.delegation-debug
		 [:hr]
		 [:div.edit-mode?*
			[:h3 "@edit-mode?*"]
			[:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.delegation-id
      [:h3 "@delegation-id*"]
      [:pre (with-out-str (pprint @delegation-id*))]]
		 [:div.delegation-data
			[:h3 "@delegation-data*"]
			[:pre (with-out-str (pprint @delegation-data*))]]
		 [:div.resonsible-user-id
			[:h3 "@responsible-user-id*"]
			[:pre (with-out-str (pprint @responsible-user-id*))]]
		 [:div.resonsible-user-data
			[:h3 "@responsible-user-data*"]
			[:pre (with-out-str (pprint @responsible-user-data*))]]]))

;; responsible user component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-delegation-resonsible-user-input-append-component []
  [:span.input-group-text
   (if-let [responsible-user-data @responsible-user-data*]
     [:a {:href (path :user {:user-id (:id responsible-user-data)})}
      (:firstname responsible-user-data)
      " " (:lastname responsible-user-data)]
     [:span.text-muted "..."])])

(defn edit-delegation-resonsible-user-input-append-component []
  [:a.btn.btn-outline-secondary
   {:href (path :delegation-edit-choose-responsible-user
                {:delegation-id (or @delegation-id* "")}
                {:delegation-name (:name @delegation-data*)})}
   " Choose responsible user "])

(defn new-delegation-resonsible-user-input-append-component []
  [:a.btn.btn-outline-secondary
   {:href (path :delegation-add-choose-responsible-user
                {}
                {:delegation-name (:name @delegation-data*)})}
   " Choose responsible user "])

(defn responsible-user-component []
  [:div.form-group.row
   [:label.col.col-form-label.col-sm-2 {:for :responsible_user_id} :responsible_user_id]
   [:div.col.input-group.mb-3
    [:input.form-control
     {:type :text
      :disabled (not @edit-mode?*)
      :value (:responsible_user_id @delegation-data*)
      :on-change #(swap! delegation-data* assoc :responsible_user_id (-> % .-target .-value presence))}]
    [:div.input-group-append
     (case (:handler-key @state/routing-state*)
       :delegation [show-delegation-resonsible-user-input-append-component]
       :delegation-edit [edit-delegation-resonsible-user-input-append-component]
       :delegation-add [new-delegation-resonsible-user-input-append-component])]]])

(defn choose-responsible-user-th-component []
  [:th {:key :choose} "Choose"])

(defn choose-responsible-user-td-component [user]
  [:td {:key :choose}
   [:a.btn.btn-sm.btn-outline-secondary
    {:href (if @delegation-id*
             (path :delegation-edit
                   {:delegation-id @delegation-id*}
                   {:responsible_user_id (:id user)
                    :name (-> @state/routing-state* :query-params :delegation-name)})
             (path :delegation-add
                   {}
                   {:responsible_user_id (:id user)
                    :name (-> @state/routing-state* :query-params :delegation-name)}))}
    "Choose as responsible user"]])


(def colconfig  
  (merge users/default-colconfig
         {:email false
          :customcols [{:key :choose
                        :th choose-responsible-user-th-component
                        :td choose-responsible-user-td-component}]}))

(defn choose-responsible-user-page []
  [:div
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)
      (when @delegation-id*
        (breadcrumbs/delegation-li @delegation-id*))
      (if @delegation-id*
        (breadcrumbs/delegation-edit-li @delegation-id*)
        (breadcrumbs/delegation-add-li))]
     [])
   [:h1 "Choose Responsible User"]
   [users/main-page-content-component colconfig]])


;; delegation components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [field-component :name]
   [responsible-user-component]
   ])

(defn additional-properties-component []
  [:div.additional-properties
   [:p [:span "This delegation has been created " [humanize-datetime-component (:created_at @delegation-data*)]
        ", and updated "[humanize-datetime-component (:updated_at @delegation-data*)]
        ". "]
    (let [c (:contracts_count @delegation-data*)]
      (if (< c 1)
        [:span "It has no contracts."]
        [:span "It has " c " " (pluralize-noun c "contract")  ". "]))]])

(defn delegation-component []
  [:div.delegation-component
   (if (nil?  @delegation-data*)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [:div
      [:div.row.mt-4
       [:div.col-lg-12.mt-2 [basic-component]]]
      [:div.row.mt-4 ]
      (when-not @edit-mode?*
        [additional-properties-component])])])

(defn delegation-name-component []
  (if-not @delegation-data*
    [:span {:style {:font-family "monospace"}} (short-id @delegation-id*)]
    [:em (str (:name @delegation-data*))]))

(defn delegation-id-component []
  [:p "delegation id: " [:span {:style {:font-family "monospace"}} (:id @delegation-data*)]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.delegation
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)
      (breadcrumbs/delegation-li @delegation-id*)]
     [(breadcrumbs/delegation-users-li @delegation-id*)
      (breadcrumbs/delegation-delete-li @delegation-id*)
      (breadcrumbs/delegation-edit-li @delegation-id*)])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Delegation "]
      [delegation-name-component]]
     [delegation-id-component]]]
   [delegation-component]
   [debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :delegation {:delegation-id @delegation-id*})
                               :method :patch
                               :json-params  @delegation-data*}
                              {:modal true
                               :title "Update Delegation"
                               :handler-key :delegation-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :delegation {:delegation-id @delegation-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-delegation
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)
      (breadcrumbs/delegation-li @delegation-id*)
      (breadcrumbs/delegation-edit-li @delegation-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Delegation "]
      [delegation-name-component]]
     [delegation-id-component]]]
   [delegation-component]
   [patch-submit-component]
   [debug-component]])


;;; new  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :delegations)
                               :method :post
                               :json-params  @delegation-data*}
                              {:modal true
                               :title "Create Delegation"
                               :handler-key :delegation-add
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :delegation {:delegation-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Create "]]
     [:div.clearfix]]))

(defn new-page []
  [:div.new-delegation
   [state/hidden-routing-state-component
    {:will-mount #(reset! delegation-data*
                          (merge {} (:query-params @state/routing-state*)))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)
      (breadcrumbs/delegation-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " New Delegation "]]]]
   [delegation-component]
   [create-submit-component]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transfer-data* (reagent/atom {}))

(defn delete-delegation [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :delegation (-> @state/routing-state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete Delegation"
                               :handler-key :delegation-delete
                               :retry-fn #'delete-delegation}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :delegations {}
                    (-> @state/global-state* :delegations-query-params))))))))

(defn delete-without-reasignment-component []
  [:div.card.m-3
   [:div.card-header.bg-warning
    [:h2 "Delete Delegation"]]
   [:div.card-body
    [:p.text-warning
     "If the to be deleted delegation is associated with other entities "
     " (such as contracts e.g.) it is not possible to delete the delegation. "
     " The delete operation will fail then without altering any data. "]
    [:div.float-right
     [:button.btn.btn-warning.btn-lg
      {:on-click delete-delegation}
      [:i.fas.fa-times] " Delete"]]]])

(defn transfer-data-and-delete-delegation [_]
  (let [resp-chan (async/chan)
        url (path :delegation-transfer-data
                  {:delegation-id @delegation-id*
                   :target-delegation-id (:target-delegation-id @transfer-data*)})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:title "Transfer Data and Delete Delegation"
              :handler-key :delegation-delete
              :retry-fn #'transfer-data-and-delete-delegation}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :delegations {}
                    (-> @state/global-state* :delegations-query-params))))))))

(defn delete-page []
  [:div.delegation-delete
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/delegations-li]
      [breadcrumbs/delegation-li @delegation-id*]
      [breadcrumbs/delegation-delete-li @delegation-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete Delegation "
    [delegation-name-component]]
   [delegation-id-component]
   [delete-without-reasignment-component]
   ])


