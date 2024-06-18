(ns leihs.admin.resources.groups.group.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.groups.group.core :as core :refer [clean-and-fetch data*
                                                             debug-component]]
   [leihs.admin.resources.groups.group.delete :as delete]
   [leihs.admin.resources.groups.group.edit :as edit]
   [leihs.admin.resources.groups.group.inventory-pools :as inventory-pools]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Nav]]))

(defn properties-table []
  [table/container
   {:className "properties"
    :borders false
    :header [:tr [:th "Property"] [:th.w-75 "Value"]]
    :body
    [:<>
     [:tr.name
      [:td "Name" [:small " (name)"]]
      [:td (:name @data*)]]
     [:tr.description
      [:td "Description" [:small " (description)"]]
      [:td {:style {:white-space "break-spaces"}} (:description @data*)]]
     [:tr.admin_protected
      [:td "Admin Protected" [:small " (admin_protected"]]
      [:td (if (:admin_protected @data*) "yes " "no")]]
     [:tr.system_admin_protected
      [:td "System-admin Protected" [:small " (system_admin_protected)"]]
      [:td (if (:system_admin_protected @data*) "yes " "no")]]
     [:tr.organization
      [:td "Organization" [:small " (organization)"]]
      [:td (:organization @data*)]]
     [:tr.org_id
      [:td "Org ID" [:small " (org_id)"]]
      [:td (:org_id @data*)]]
     [:tr.user_counts
      [:td "Number of Users" [:small " (users_count)"]]
      [:td (:users_count @data*)]]]}])

(defn header []
  (let [name (:name @data*)]
    (fn []
      [:header.my-5
       [breadcrumbs/main]
       [:h1.mt-3 name]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(do
                   (reset! inventory-pools/data* nil)
                   (core/fetch))}]

   (if-not @data*
     [:div.my-5
      [wait-component]]

     [:article.group
      [header]

      [:section
       [properties-table]
       [edit/button]
       [delete/button]]

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

       [:section
        [inventory-pools/table-component]]
       [delete/dialog]
       [edit/dialog]
       [debug-component]]])])
