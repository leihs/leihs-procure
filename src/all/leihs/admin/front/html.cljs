(ns leihs.admin.front.html
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.requests.modal]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.env :refer [use-remote-navbar?]]

    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.admin.front :as admin :refer [page] :rename {page home-page}]

    [clojure.pprint :refer [pprint]]
    [accountant.core :as accountant]
    [reagent.core :as reagent]
    ))

(defn li-navitem [handler-key display-string]
  (let [active? (= (-> @routing/state* :handler-key) handler-key)]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path handler-key)} display-string]]))

(defn li-admin-navitem []
  (let [active? (boolean
                  (when-let [current-path (-> @routing/state* :path)]
                    (re-matches #"^/admin.*$" current-path)))]
    [:li.nav-item
     {:class (if active? "active" "")}
     [:a.nav-link {:href (path :admin)} "Admin"]]))

(defn sign-out-nav-component []
  [:form.form-inline.ml-2
   {:action (path :auth-sign-out {} {:target (-> @routing/state* :url)})
    :method :post}
   [:div.form-group
    [:input
     {:name :url
      :type :hidden
      :value (-> @routing/state* :url)}]]
   [anti-csrf/hidden-form-group-token-component]
   [:div.form-group
    [:label.sr-only
     {:for :sign-out}
     "Sign out"]
    [:button#sign-out.btn.btn-dark.form-group
     {:type :submit
      :style {:padding-top "0.2rem"
              :padding-bottom "0.2rem"}}
     [:span
      [:span " Sign out "]
      [:i.fas.fa-sign-out-alt]]]]])

(defn nav-bar []
  [:nav.navbar.navbar-expand.justify-content-between
   {:class (if (= (-> @routing/state* :handler-key) :home)
             "navbar-light bg-light" "navbar-dark bg-admin")}
   [:a.navbar-brand {:href (path :home), :data-trigger true} "leihs"]
   [:div
    (when @core-user/state*
      [:ul.navbar-nav
       [li-admin-navitem]
       [li-navitem :borrow "Borrow"]
       [li-navitem :lending "Lending"]
       [li-navitem :procure "Procurement"]
       ])]
   [core-user/navbar-user-nav]])

(defn version-component []
  [:span.navbar-text "Version "
   (let [major (:version_major @state/leihs-admin-version*)
         minor (:version_minor @state/leihs-admin-version*)
         patch (:version_patch @state/leihs-admin-version*)
         pre (:version_pre @state/leihs-admin-version*)
         build (:version_build @state/leihs-admin-version*)]
     [:span
      [:span.major major]
      "." [:span.minor minor]
      "." [:span.patch patch]
      (when pre
        [:span "-"
         [:span.pre pre]])
      (when build
        [:span "+"
         [:span.build build]])])])

(defn current-page []
  [:div
   [leihs.core.requests.modal/modal-component]
   (if-not (use-remote-navbar?) [nav-bar])
   [:div
    (if-let [page (:page @routing/state*)]
      [page]
      [:div.page
       [:h1.text-danger "Application error: the current path can not be resolved!"]])]
   [state/debug-component]
   [:nav.footer.navbar.navbar-expand-lg.navbar-dark.bg-secondary.col.mt-4
    [:div.col
     [:a.navbar-brand {:href (path :admin {})} "leihs-admin"]
     [version-component]]
    [:div.col
     [:a.navbar-text
      {:href (path :status)} "Admin-Status-Info"]]
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
