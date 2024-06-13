(ns leihs.admin.resources.users.user.main
  (:require
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.user.api-tokens.main :as api-tokens]
   [leihs.admin.resources.users.user.core :as user-core :refer [clean-and-fetch user-data*
                                                                user-id*]]
   [leihs.admin.resources.users.user.delete :as delete]
   [leihs.admin.resources.users.user.edit :as edit]
   [leihs.admin.resources.users.user.groups :as groups]
   [leihs.admin.resources.users.user.inventory-pools :as inventory-pools]
   [leihs.admin.resources.users.user.password-reset.main :as password-reset]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Button ButtonGroup DropdownButton Dropdown Tabs Tab]]
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

(defn edit-user-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed? [modifieable?])
        [:<>
         [:> Button
          {:onClick #(reset! show true)}
          "Edit User"]
         [edit/dialog {:show @show
                       :onHide #(reset! show false)}]]))))

(defn reset-password-button []
  (let [show (reagent/atom false)]
    (fn []
      (when (auth/allowed? [modifieable?])
        [:<>
         [:> DropdownButton {:as ButtonGroup :title "Reset Password"}
          [:> Dropdown.Item {:on-click #(do (reset! show true)
                                            (when (string? @password-reset/user-password-resetable?*)
                                              (password-reset/submit :valid_for 1)))}
           "Create Reset Link - 1 hour"]
          [:> Dropdown.Item {:on-click #(do (reset! show true)
                                            (when (string? @password-reset/user-password-resetable?*)
                                              (password-reset/submit)))}
           "Create Reset Link - 24 hours"]
          [:> Dropdown.Item {:on-click #(do (reset! show true)
                                            (when (string? @password-reset/user-password-resetable?*)
                                              (password-reset/submit :valid_for (* 3 24))))}
           "Create Reset Link - 3 days"]
          [:> Dropdown.Item {:on-click #(do (reset! show true)
                                            (when (string? @password-reset/user-password-resetable?*)
                                              (password-reset/submit {:valid_for (* 7 24)})))}
           "Create Reset Link - 7 days"]]
         [password-reset/dialog  {:show @show
                                  :onHide #(reset! show false)}]]))))

;; NOTE: This is a workaround and should be fixed
;; currently the issue is that a user is selected by navigating to the user route
;; on mount it is checked if user uid exists in query params
;; if so the modal opens
;; it would be nicer if the user selection would happen in the modal with a e.g. a combobox
(def show* (reagent/atom false))

(defn check-user-chosen []
  (when (contains?
         (get @routing/state* :query-params) :user-uid)
    (reset! show* true)))

(defn delete-button []
  (when (auth/allowed? [modifieable?])
    [:<>
     [:> Button
      {:className "ml-3"
       :variant "danger"
       :onClick #(reset! show* true)}
      "Delete User"]]))

(defn delete-user-dialog []
  [delete/dialog {:show @show*
                  :onHide #(reset! show* false)}])

(defn basic-properties []
  (let [data @user-data*]
    (fn []
      [:div.basic-properties.mb-3
       [:h2 "Basic User Properties"]
       [:div.row
        [:div.col-md-3.mb-2
         [:hr]
         [:h3 " Image / Avatar "]
         [user-core/img-avatar-component data]]
        [:div.col-md
         [:hr]
         [:h3 "Personal Properties"]
         [user-core/personal-properties-component data]]
        [:div.col-md
         [:hr]
         [:h3 "Account Properties"]
         [user-core/account-properties-component data]]]
       [:div.mt-3
        [:> ButtonGroup {:className "mr-3"}
         [edit-user-button]
         [reset-password-button]]
        [delete-button]]])))

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
  (if (empty? @user-data*)
    [:div.mt-5
     [wait-component]
     [routing/hidden-state-component
      {:did-mount #(do
                     (clean-and-fetch)
                     (check-user-chosen))}]]

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
     [delete-user-dialog]
     [user-core/debug-component]]))
