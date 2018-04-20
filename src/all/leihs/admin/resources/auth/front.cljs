(ns leihs.admin.resources.auth.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.anti-csrf.core :as anti-csrf]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.icons :as icons]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def password-sign-in* (reagent/atom {}))

(def current-sign-in-component* (reagent/atom nil))

(defn shib-sign-in-button-component []
  [:a.btn.btn-primary.mx-2 {:href (-> @state/settings* :shibboleth_login_path)}
   icons/sign-in " Sign in via SwitchAAI"])

(defn password-sign-in-nav-component []
  [:form.form-inline.my-2.my-lg-0
   {:method :post
    :action (path :auth-password-sign-in {} {:target (or (-> @state/routing-state* :query-params :target presence)
                                                         (-> @state/routing-state* :url))})}
   [anti-csrf/hidden-form-group-token-component]
   [:button.btn.btn-warning.mx-2
    {:type :button
     :on-click #(reset! current-sign-in-component* nil)}
    [:span icons/abort " Abort " ]]
   [:div.form-group
    [:input
     {:name :url
      :type :hidden
      :value (-> @state/routing-state* :url)}]]
   [:input#email.form-control.mr-sm-2 
    {:type :email
     :name :email
     :placeholder "email address"
     :value (:email @password-sign-in*)
     :on-change #(swap! password-sign-in* assoc :email (-> % .-target .-value presence))}]
   [:input#password.form-control.mr-sm-2
    {:type :password
     :name :password
     :placeholder "password"
     :value (:password @password-sign-in*)
     :on-change #(swap! password-sign-in* assoc :password (-> % .-target .-value presence))}]
   [:button#sign-in-with-password.btn.btn-primary.form-group
    {:type :submit}
    [:span " Sign in " icons/sign-in]]])

(defn nav-sign-in-component []
  (if-let [current-sign-in-component @current-sign-in-component*]
    (case @current-sign-in-component* 
      :password [password-sign-in-nav-component]
      [:div])
    [:div.form-inline
     (when (:shibboleth_enabled @state/settings*)
       [shib-sign-in-button-component])
     [:button.btn.btn-secondary.mx-2
      {:on-click #(reset! current-sign-in-component* :password)}
       icons/sign-in " Sign in with password "]]))


(defn password-sign-in-component []
  [:div
   [:h2 "Sign in with password"]
   [:form.form
    {:method :post
     :action (path :auth-password-sign-in)}
    [anti-csrf/hidden-form-group-token-component]
    [:div.form-group
     [:input
      {:name :url
       :type :hidden
       :value (-> @state/routing-state* :url)}]]
    [:div.form-group
     [:label {:for :email} "Email: "]
     [:div
      [:input.form-control
       {:id :email
        :name :email
        :type :email
        :value (:email @password-sign-in*)
        :on-change #(swap! password-sign-in* assoc :email (-> % .-target .-value presence))}]]]
    [:div.form-group
     [:label {:for :password} "Password: "]
     [:div
      [:input.form-control
       {:id :password
        :name :password
        :type :password
        :value (:password @password-sign-in*)
        :on-change #(swap! password-sign-in* assoc :password (-> % .-target .-value presence))}]]]
    [:div.form-group.float-right
     [:button.btn.btn-primary
      {:type :submit}
      [:i.fas.fa-sign-in-alt] " Sign in"]]]
   [:div.clearfix]])



(defn shib-sign-in-component []
  (when (-> @state/settings* :shibboleth_enabled)
    [:div
     [:h2 "Sign in via Shibboleth / SwitchAAI"]
     [:div.float-right
      [shib-sign-in-button-component]]
     [:div.clearfix]]))


(defn sign-in-form-component []
  [:div
   [shib-sign-in-component]
   [password-sign-in-component]
   ])

(defn password-sign-in-page []
  (reagent/create-class
    {:component-did-mount #(reset! password-sign-in* {})
     :reagent-render
     (fn [_]
       [:div.password-sign-in
        (breadcrumbs/nav-component
          [(breadcrumbs/leihs-li)
           (breadcrumbs/admin-li)
           (breadcrumbs/auth-li)
           (breadcrumbs/auth-password-sign-in-li)
           ][])
        [sign-in-form-component]
        ])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce auth-data* (reagent/atom nil))

(def fetch-auth-id* (reagent/atom nil))

(defn fetch-auth []
  (reset! auth-data* nil)
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :auth)
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Authentication"
                               :handler-key :auth
                               :retry-fn #'fetch-auth}
                              :chan resp-chan)]
    (reset! fetch-auth-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-auth-id*))
            (reset! auth-data* (:body resp)))))))


(defn auth-page []
  (reagent/create-class
    {:component-did-mount #(fetch-auth)
     :reagent-render
     (fn [_]
       [:div.session
        (breadcrumbs/nav-component
          [(breadcrumbs/leihs-li)
           (breadcrumbs/admin-li)
           (breadcrumbs/auth-li)]
          [(breadcrumbs/auth-password-sign-in-li)])
        [:h1 "Authentication"]
        [:p "The data shown below is mostly of interest for exploring the API or for debugging."]
        (when-let [auth-data @auth-data*]
          [:pre.bg-light
           (with-out-str (pprint auth-data))])])}))


