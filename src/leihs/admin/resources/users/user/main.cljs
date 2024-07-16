(ns leihs.admin.resources.users.user.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.users.user.api-tokens.main :as api-tokens]
   [leihs.admin.resources.users.user.core :as user-core :refer [fetch
                                                                user-data*]]
   [leihs.admin.resources.users.user.delete :as delete]
   [leihs.admin.resources.users.user.edit :as edit]
   [leihs.admin.resources.users.user.groups :as groups]
   [leihs.admin.resources.users.user.inventory-pools :as inventory-pools]
   [leihs.admin.resources.users.user.password-reset.main :as password-reset]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.admin.utils.search-params :as search-params]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [ButtonGroup Dropdown
                                                DropdownButton Tab Tabs]]
   [reagent.core :as reagent]))

(defn modifieable? [current-user-state _]
  (cond
    (auth/system-admin-scopes?
     current-user-state _) true
    (auth/admin-scopes?
     current-user-state
     _)  (cond (or (nil? @user-data*) (:is_system_admin @user-data*)) false
               (or (nil? @user-data*) (:system_admin_protected @user-data*)) false
               :else true)
    :else (cond (or (nil? @user-data*) (:is_admin @user-data*)) false
                (or (nil? user-data*) (:admin_protected @user-data*)) false
                :else true)))

(defn reset-password-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed? [modifieable?])
        [:<>
         [password-reset/dialog]
         [:> DropdownButton {:as ButtonGroup
                             :title "Reset Password"}
          [:> Dropdown.Item
           {:on-click #(do (search-params/append-to-url
                            {:action "reset-password"
                             :valid-for 1})
                           (when (string? @password-reset/user-password-resetable?*)
                             (password-reset/get-reset-data)))}
           "Create Reset Link - 1 hour"]

          [:> Dropdown.Item
           {:on-click #(do (search-params/append-to-url
                            {:action "reset-password"
                             :valid-for (* 1 24)})
                           (when (string? @password-reset/user-password-resetable?*)
                             (password-reset/get-reset-data)))}
           "Create Reset Link - 24 hours"]

          [:> Dropdown.Item
           {:on-click #(do (search-params/append-to-url
                            {:action "reset-password"
                             :valid-for (* 3 24)})
                           (when (string? @password-reset/user-password-resetable?*)
                             (password-reset/get-reset-data)))}
           "Create Reset Link - 3 days"]

          [:> Dropdown.Item
           {:on-click #(do
                         (search-params/append-to-url
                          {:action "reset-password"
                           :valid-for (* 7 24)})
                         (when (string? @password-reset/user-password-resetable?*)
                           (password-reset/get-reset-data)))}
           "Create Reset Link - 7 days"]]]))))

(defn basic-properties []
  [:div.basic-properties.mb-3
   [:h2 "Basic User Properties"]
   [:div.row
    [:div.col-md-3.mb-2
     [:hr]
     [:h3 " Image / Avatar "]
     [user-core/img-avatar-component @user-data*]]
    [:div.col-md
     [:hr]
     [:h3 "Personal Properties"]
     [user-core/personal-properties-component @user-data*]]
    [:div.col-md
     [:hr]
     [:h3 "Account Properties"]
     [user-core/account-properties-component @user-data*]]]
   [:div.mt-3
    [:> ButtonGroup {:className "mr-3"}
     [edit/button]
     [reset-password-button]]
    [delete/button]]])

(defn header []
  (let [name (str (:firstname @user-data*)
                  " "
                  (:lastname @user-data*))]
    (fn []
      [:header.my-5
       [breadcrumbs/main]
       [:h1.mt-3 name]])))

(defn own-user-admin-scopes? [user-state _routing-state]
  (and (= (-> user-state :id)
          (-> _routing-state :route-params :user-id))
       (auth/admin-scopes? user-state _routing-state)))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(fetch)}]

   (if-not @user-data*
     [:div.mt-5
      [wait-component]]
     [:article.users
      [header]
      [basic-properties]

      [:> Tabs {:className "mt-5"
                :defaultActiveKey "inventory-pools"
                :transition false}
       [:> Tab {:eventKey "inventory-pools" :title "Inventory Pools"}
        [inventory-pools/table-component {:chrome false}]]
       [:> Tab {:eventKey "groups" :title "Groups"}
        [groups/table-component]]
       (when (auth/allowed?
              [auth/system-admin-scopes? own-user-admin-scopes?])
         [:> Tab {:eventKey "api-tokens" :title "API Tokens"}
          [api-tokens/table-component]])]

      [delete/dialog]
      [edit/dialog]
      [user-core/debug-component]])])
