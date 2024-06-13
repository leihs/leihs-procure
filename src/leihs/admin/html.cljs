(ns leihs.admin.html
  (:require
   ["/admin-ui" :as UI]
   ["react-bootstrap" :as BS]
   [accountant.core :as accountant]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.http-client.modals]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.constants :as constants]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.sidebar :as sidebar]
   [leihs.admin.state :as state :refer [global-state*] :rename {global-state* state*}]
   [leihs.core.dom :as dom]
   [leihs.core.routing.front :as routing]
   [reagent.dom :as rdom]))

(defn footer []
  [:div
   [:> BS/Navbar {:bg :dark :variant :dark}
    [:div.container-fluid
     [:> BS/Navbar.Text {}
      [:a {:href constants/REPOSITORY_URL}
       [icons/github] " leihs/admin "]
      (when-let [commit-id (some-> @state* :server-state :built-info :commit_id)]
        [:<> [:span " @ "]
         [:a {:href (str constants/REPOSITORY_URL "/commit/" commit-id)}
          (subs commit-id 0 5)]])]
     [:> BS/Navbar.Text {}
      [:a {:href (path :status)} "Admin-Status-Info"]]
     [state/debug-toggle-navbar-component]]]])

(defn main []
  (if-let [page (:page @routing/state*)]
    [:div.mx-5
     [page]
     [breadcrumbs/watcher]]
    [:div.page
     [:h1.text-danger
      [:b "Error 404 - There is no handler for the current path defined."]]
     [state/debug-component]]))

(defn current-page []
  [:<>
   [leihs.admin.common.http-client.modals/modal-component]
   (let [navbar-data (dom/data-attribute "body" "navbar")]
     [:> UI/Components.Layout
      [:> UI/Components.Layout.Header
       [:> UI/Components.Navbar navbar-data]]
      [:> UI/Components.Layout.Aside
       (sidebar/sidebar)]
      [:> UI/Components.Layout.Main
       [main]]
      [:> UI/Components.Layout.Footer
       [footer]]])])

(defn mount []
  (when-let [app (.getElementById js/document "app")]
    (rdom/render [current-page] app))
  (accountant/dispatch-current!))
