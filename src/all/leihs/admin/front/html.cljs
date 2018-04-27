(ns leihs.admin.front.html
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.admin.resources.auth.front :as auth]
    [leihs.admin.anti-csrf.core :as anti-csrf]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.requests.modal]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state :refer [routing-state*]]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.admin.front :as admin :refer [page] :rename {page home-page}]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [clojure.pprint :refer [pprint]]
    [accountant.core :as accountant]
    [reagent.core :as reagent]
    ))

(defn li-navitem [handler-key display-string]
  (let [active? (= (-> @state/routing-state* :handler-key) handler-key)]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path handler-key)} display-string]]))

(defn li-admin-navitem []
  (let [active? (boolean
                  (when-let [current-path (-> @state/routing-state* :path)]
                    (re-matches #"^/admin.*$" current-path)))]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path :admin)} "Admin"]]))

(defn sign-out-nav-component []
  [:form.form-inline.ml-2
   {:action (path :auth-sign-out {} {:target (-> @state/routing-state* :url)})
    :method :post}
   [:div.form-group
    [:input
     {:name :url
      :type :hidden
      :value (-> @state/routing-state* :url)}]]
   [anti-csrf/hidden-form-group-token-component]
   [:div.form-group
    [:label.sr-only
     {:for :sign-out}
     "Sign out"]
    [:button#sign-out.btn.btn-dark.form-group
     {:style {:padding 7}
      :type :submit}
     [:i.fas.fa-sign-out-alt]
     [:span.sr-only "Sign out"]]]])

(defn navbar-user-nav []
  (if-let [user @state/user*]
    [:div.navbar-nav.user-nav
     [:div
      [:a
       {:href (path :user {:user-id (:id user)} {})}
       [:span
        [:img.user-img-32
         {:width 32
          :height 32
          :src (or (:img32_url user)
                   (gravatar-url (:email user)))}]
        [:span.sr-only (:email user)]]]]
     [sign-out-nav-component]]
    [:div.navbar-nav
     [auth/nav-sign-in-component]
     ]))

(defn nav-bar []
  [:nav.navbar.navbar-expand.justify-content-between
   {:class (if (= (-> @state/routing-state* :handler-key) :home)
             "navbar-light bg-light" "navbar-dark bg-admin")}
   [:a.navbar-brand {:href (path :home)} "leihs"]
   [:div
    (when @state/user*
      [:ul.navbar-nav
       [li-admin-navitem]
       [li-navitem :borrow "Borrow"]
       [li-navitem :lending "Lending"]
       [li-navitem :procure "Procurement"]
       ])]
   [navbar-user-nav]])

(defn current-page []
  [:div
   [leihs.admin.front.requests.modal/modal-component]
   [nav-bar]
   [:div
    (if-let [page (:page @routing-state*)]
      [page]
      [:div.page
       [:h1.text-danger "Application error: the current path can not be resolved!"]])]
   [state/debug-component]
   [:nav.navbar.navbar-expand-lg.navbar-dark.bg-secondary.col
    {:style {:margin-top "3em"}}
    [:div.col
     [:a.navbar-brand {:href (path :admin {})} "leihs-admin"]
     [:span.navbar-text "Version 0.0.0 Alpha"]]
    [state/debug-toggle-navbar-component]
    [:form.form-inline {:style {:margin-left "0.5em"
                                :margin-right "0.5em"}}
     [:label.navbar-text
      [:a {:href (path :requests)}
       [requests/icon-component]
       " Requests "]]]]])

(defn mount []
  (when-let [app (.getElementById js/document "app")]
    (reagent/render [current-page] app))
  (accountant/dispatch-current!))
