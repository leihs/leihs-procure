(ns leihs.admin.resources.system.authentication-system.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.url.shared]

    [leihs.admin.front.breadcrumbs :as admin-breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component wait-component]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.system.breadcrumbs :as system-breadcrumbs]
    [leihs.core.icons :as icons]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defonce authentication-system-id* (reaction (-> @routing/state* :route-params :authentication-system-id)))
(defonce authentication-system-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @authentication-system-data*)
         (boolean ((set '(:authentication-system-edit :authentication-system-add))
                   (:handler-key @routing/state*))))))

(def fetch-authentication-system-id* (reagent/atom nil))
(defn fetch-authentication-system []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Authentication-System"
                               :handler-key :authentication-system
                               :retry-fn #'fetch-authentication-system}
                              :chan resp-chan)]
    (reset! fetch-authentication-system-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-authentication-system-id*))
            (reset! authentication-system-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! authentication-system-data* nil)
  (fetch-authentication-system))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text :node-type :input} opts)]
     [:div.form-group.row
      [:label.col.col-form-label.col-sm-2 {:for kw} kw]
      [:div.col.col-sm-10
       [:div.input-group
        (if @edit-mode?*
          [(:node-type opts)
           {:class :form-control
            :id kw
            :type (:type opts)
            :value (or (kw @authentication-system-data*) "")
            :on-change #(swap! authentication-system-data* assoc kw
                               (some-> % .-target .-value presence
                                       ((if (= (:type opts) :number)
                                          leihs.core.url.shared/parse-int
                                          identity))))
            :disabled (not @edit-mode?*)}]
          [:div
           (if-let [value (-> @authentication-system-data* kw presence)]
             [:span.form-control-plaintext
              (case (:type opts)
                :email [:a {:href (str "mailto:" value)}
                        [:i.fas.fa-envelope] " " value]
                :url [:a {:href value} value]
                (if (clojure.string/ends-with? (str kw) "key")
                  [:span (apply str  (take 40 value)) " ..."]
                  value ))])])]]])))

(defn checkbox-component [kw]
  [:div.form-check.form-check-inline
   [:label {:for kw}
    [:input
     {:id kw
      :type :checkbox
      :checked (kw @authentication-system-data*)
      :on-change #(swap! authentication-system-data* assoc kw (-> @authentication-system-data* kw boolean not))
      :disabled (not @edit-mode?*)}]
    [:span.ml-2 kw]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.authentication-system-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.authentication-system-data
      [:h3 "@authentication-system-data*"]
      [:pre (with-out-str (pprint @authentication-system-data*))]]]))


;; authentication-system components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [:h3 "Basic properties"]
   (when (= (-> @routing/state* :handler-key) :authentication-system-add)
     [field-component :id])
   [field-component :name]
   [field-component :type {}]
   [field-component :description {:node-type :textarea}]
   [checkbox-component :enabled]
   [checkbox-component :send_email]
   [checkbox-component :send_org_id]
   [checkbox-component :send_login]
   [field-component :priority {:type :number}]
   [field-component :internal_private_key {:node-type :textarea}]
   [field-component :internal_public_key {:node-type :textarea}]
   [field-component :external_public_key {:node-type :textarea}]
   [field-component :external_sign_in_url {}]
   [field-component :external_sign_out_url {}]])

(defn count-component [kw noun data]
  (let [count (kw data)]
    [:span
     "This authentication-system has "
     [:strong count " " (pluralize-noun count noun)] ". "]))

(defn additional-properties-component []
  (when-let [authentication-system-data @authentication-system-data*]
    [:div.additional-properties
     [:p
      [:span "This authentication-system has been "
       [:b " created " [humanize-datetime-component (:created_at authentication-system-data)]] ", and "
       [:b " updated " [humanize-datetime-component (:updated_at authentication-system-data)]] ". "]
      (count-component :users_count "user" authentication-system-data)
      (count-component :groups_count "group" authentication-system-data)]]))

(defn authentication-system-component []
  [:div.authentication-system-component
   (if (nil?  @authentication-system-data*)
     [wait-component]
     [:div
      [:div [basic-component]]
      [:div [additional-properties-component]]])])

(defn name-component []
  (if-not @authentication-system-data*
    [:span {:style {:font-family "monospace"}} (short-id @authentication-system-id*)]
    [:em (str (:name @authentication-system-data*))]))

(defn authentication-system-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @authentication-system-data*)]])

(defn org-warning-component []
  (when (:org_id @authentication-system-data*)
    [:div.alert.alert-warning
     [:p "The property " [:code "org_id"]
      " is conventionally reserved for authentication-systems maintained by an automated import or sync via the API. "
      " If this instance of leihs is using such a mechanism it will likely " [:b "override any changes submitted here." ]]]))

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.authentication-system
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (admin-breadcrumbs/nav-component
     [(admin-breadcrumbs/leihs-li)
      (admin-breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (breadcrumbs/authentication-systems-li)
      (breadcrumbs/authentication-system-li)]
     [(breadcrumbs/authentication-system-users-li)
      (breadcrumbs/authentication-system-groups-li)
      (breadcrumbs/authentication-system-delete-li)
      (breadcrumbs/authentication-system-edit-li)])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Authentication-System "]
      [name-component]]
     [authentication-system-id-component]]]
   [authentication-system-component]
   [debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system {:authentication-system-id @authentication-system-id*})
                               :method :patch
                               :json-params  (dissoc @authentication-system-data* :users_count)}
                              {:modal true
                               :title "Update Authentication-System"
                               :handler-key :authentication-system-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :authentication-system {:authentication-system-id @authentication-system-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [org-warning-component]
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-authentication-system
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (admin-breadcrumbs/nav-component
     [(admin-breadcrumbs/leihs-li)
      (admin-breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (breadcrumbs/authentication-systems-li)
      (breadcrumbs/authentication-system-li)
      (breadcrumbs/authentication-system-edit-li)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Authentication-System "]
      [name-component]]
     [authentication-system-id-component]]]
   [authentication-system-component]
   [patch-submit-component]
   [debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-systems)
                               :method :post
                               :json-params  @authentication-system-data*}
                              {:modal true
                               :title "Add Authentication-System"
                               :handler-key :authentication-system-add
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :authentication-system {:authentication-system-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [org-warning-component]
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Add "]]
     [:div.clearfix]]))

(defn add-page []
  [:div.new-authentication-system
   [routing/hidden-state-component
    {:did-mount #(reset! authentication-system-data* {})}]
   (admin-breadcrumbs/nav-component
     [(admin-breadcrumbs/leihs-li)
      (admin-breadcrumbs/admin-li)
      (system-breadcrumbs/system-li)
      (breadcrumbs/authentication-systems-li)
      (breadcrumbs/authentication-system-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Add Authentication-System "]]]]
   [authentication-system-component]
   [create-submit-component]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transfer-data* (reagent/atom {}))

(defn delete-authentication-system [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system (-> @routing/state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete Authentication-System"
                               :handler-key :authentication-system-delete
                               :retry-fn #'delete-authentication-system}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :authentication-systems {}
                    (-> @state/global-state* :authentication-systems-query-params))))))))

(defn delete-submit-component []
  [:div.form
   [org-warning-component]
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click delete-authentication-system}
     icons/delete
     " Delete "]]
   [:div.clearfix]])

(defn delete-component []
  [:div
   [:h2 "Delete Authentication-System"]
   [:div.float-right
    [:button.btn.btn-warning.btn-lg
     {:on-click delete-authentication-system}
     [:i.fas.fa-times] " Delete"]]])

(defn delete-page []
	[:div.authentication-system-delete
	 [routing/hidden-state-component
		{:did-mount clean-and-fetch
		 :did-change clean-and-fetch}]
	 [:div.row
		[:nav.col-lg {:aria-label :breadcrumb :role :navigation}
		 [:ol.breadcrumb
			[admin-breadcrumbs/leihs-li]
			[admin-breadcrumbs/admin-li]
      [system-breadcrumbs/system-li]
      [breadcrumbs/authentication-systems-li]
			[breadcrumbs/authentication-system-li]
			[breadcrumbs/authentication-system-delete-li]]]
		[:nav.col-lg {:role :navigation}]]
	 [:h1 "Delete Authentication-System "
		[name-component]]
	 [authentication-system-id-component]
	 [delete-submit-component]])
