(ns leihs.admin.html
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    )
  (:require
    ["react-bootstrap" :as BS]
    [accountant.core :as accountant]
    [clojure.pprint :refer [pprint]]
    [leihs.admin.common.http-client.modals]
    [leihs.admin.common.icons :as icons]
    [leihs.admin.constants :as constants]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.state :as state :refer [global-state* debug?*] :rename {global-state* state*}]
    [leihs.core.anti-csrf.front :as anti-csrf]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.env :refer [use-global-navbar?]]
    [leihs.core.routing.front :as routing]
    [leihs.core.dom :as dom]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]
    [reagent.dom :as rdom]
    ["/admin-ui" :as UI]
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

(defn footer []
  [:div
   [:> BS/Navbar {:bg :secondary :variant :dark}
    [:div.container
     [:> BS/Navbar.Brand {} "leihs Admin"]
     [:> BS/Navbar.Text {}
      [:a {:href constants/REPOSITORY_URL}
       [icons/github] " leihs/admin "]
      (when-let [commit-id (some-> @state* :server-state :built-info :commit_id)]
        [:<> [:span " @ "]
         [:a {:href (str constants/REPOSITORY_URL "/commit/" commit-id)}
          (subs commit-id 0 5) ]])]
     [:> BS/Navbar.Text { }
      [:a {:href (path :status)} "Admin-Status-Info"]]
     [state/debug-toggle-navbar-component]
     ]]])


(defn current-page []
  [:div
   [leihs.admin.common.http-client.modals/modal-component]
   (let [navbar-data (dom/data-attribute "body" "navbar")]
     [:> UI/Components.Navbar navbar-data])
   [:div.container-fluid
    (if-let [page (:page @routing/state*)]
      [page]
      [:div.page
       [:h1.text-danger
        [:b "Error 404 - There is no handler for the current path defined."]]])]
   [state/debug-component]
   [footer]])

(defn mount []
  (when-let [app (.getElementById js/document "app")]
    (rdom/render [current-page] app))
  (accountant/dispatch-current!))
