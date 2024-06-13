(ns leihs.admin.resources.groups.group.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :refer [clean-and-fetch data*
                                                    debug-component]]
   [leihs.admin.resources.groups.group.delete :as delete]
   [leihs.admin.resources.groups.group.edit :as edit]
   [leihs.admin.resources.groups.group.inventory-pools :as inventory-pools]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button Nav]]
   [reagent.core :as reagent]))

(defn some-lending-manager-and-group-unprotected? [current-user-state _]
  (and (pool-auth/some-lending-manager? current-user-state _)
       (boolean  @data*)
       (and (-> @data* :admin_protected not)
            (-> @data* :system_admin_protected not))))

(defn admin-and-group-not-system-admin-protected?
  [current-user routing-state]
  (and (auth/admin-scopes? current-user routing-state)
       (-> @data* :system_admin_protected not)))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed?
             [admin-and-group-not-system-admin-protected?
              auth/system-admin-scopes?
              some-lending-manager-and-group-unprotected?])
        [:<>
         [:> Button
          {:onClick #(reset! show true)}
          "Edit"]
         [edit/dialog {:show @show
                       :onHide #(reset! show false)}]]))))

(defn delete-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed?
             [admin-and-group-not-system-admin-protected?
              auth/system-admin-scopes?
              some-lending-manager-and-group-unprotected?])
        [:<>
         [:> Button
          {:variant "danger"
           :className "ml-3"
           :onClick #(reset! show true)}
          "Delete"]
         [delete/dialog  {:show @show
                          :onHide #(reset! show false)}]]))))

(defn properties-table []
  (let [data @data*]
    (fn []
      [table/container
       {:className "properties"
        :borders false
        :header [:tr [:th "Property"] [:th.w-75 "Value"]]
        :body
        [:<>
         [:tr.name
          [:td "Name" [:small " (name)"]]
          [:td (:name data)]]
         [:tr.description
          [:td "Description" [:small " (description)"]]
          [:td {:style {:white-space "break-spaces"}} (:description data)]]
         [:tr.admin_protected
          [:td "Admin Protected" [:small " (admin_protected"]]
          [:td (if (:admin_protected data) "yes " "no")]]
         [:tr.system_admin_protected
          [:td "System-admin Protected" [:small " (system_admin_protected)"]]
          [:td (if (:system_admin_protected data) "yes " "no")]]
         [:tr.organization
          [:td "Organization" [:small " (organization)"]]
          [:td (:organization data)]]
         [:tr.org_id
          [:td "Org ID" [:small " (org_id)"]]
          [:td (:org_id data)]]
         [:tr.user_counts
          [:td "Number of Users" [:small " (users_count)"]]
          [:td (:users_count data)]]]}])))

(defn header []
  (let [name (:name @data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main]
       [:h1.mt-3 name]])))

(defn page []
  (if (empty? @data*)
    [:div.mt-5
     [wait-component]
     [routing/hidden-state-component
      {:did-mount clean-and-fetch
       :did-change clean-and-fetch}]]
    [:article.group
     [header]

     [:section
      [properties-table]
      [edit-button]
      [delete-button]]

     [:section
      [:> Nav {:variant "tabs" :className "mt-5"
               :defaultActiveKey "users"}
       [:> Nav.Item
        [:> Nav.Link {:active true} "Inventory Pools"]]

       (when (auth/allowed?
              [auth/admin-scopes?
               pool-auth/some-lending-manager?])
         [:> Nav.Item
          [:> Nav.Link
           {:href (-> (:path @routing/state*)
                      (clojure.core/str "/users/"))}
           "Users"]])]

      [:div
       [inventory-pools/table-component]]
      [debug-component]]]))
