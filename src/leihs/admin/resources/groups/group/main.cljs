(ns leihs.admin.resources.groups.group.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [leihs.admin.common.components.navigation.back :as back]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :refer [clean-and-fetch data*
                                                    debug-component
                                                    group-name-component]]
   [leihs.admin.resources.groups.group.core :refer [data*]]
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

(defn li-dl-component [dt dd]
  ^{:key dt}
  [:li {:key dt}
   [:dl.row.mb-0
    [:dt.col-sm-4 dt]
    [:dd.col-sm-8 dd]]])

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

(defn properties-component []
  [:div
   (if-not @data*
     [wait-component]
     [:<>
      [:ul.list-unstyled
       [li-dl-component "Name" (:name @data*)]
       [li-dl-component "Description " (:description @data*)]
       [li-dl-component "Admin protected"
        (if (:admin_protected @data*) "yes" "no")]
       [li-dl-component "System-admin protected"
        (if (:system_admin_protected @data*) "yes" "no")]
       [li-dl-component "Organization" (:organization @data*)]
       [li-dl-component "Org ID" (:org_id @data*)]
       [li-dl-component "Number of users" (:users_count @data*)]]
      [:div.mt-3
       [edit-button]
       [delete-button]]])])

(defn page []
  [:article.group
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:header.my-5
    [back/button  {:href (path :groups {})}]
    [:h1.mt-3 [group-name-component]]]
   [properties-component]

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
   [debug-component]])
