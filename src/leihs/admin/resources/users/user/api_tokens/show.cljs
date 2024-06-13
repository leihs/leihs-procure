(ns leihs.admin.resources.users.user.api-tokens.show
  (:require [cljs.core.async :as async :refer [<! go]]
            [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.users.user.api-tokens.core :as core]
            [leihs.admin.resources.users.user.api-tokens.delete :as delete]
            [leihs.admin.resources.users.user.api-tokens.edit :as edit]
            [leihs.admin.resources.users.user.core :as user-core]
            [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
            [leihs.core.routing.front :as routing]
            [react-bootstrap :as react-bootstrap :refer [Button Table]]
            [reagent.core :as reagent :refer [reaction]]))

(defonce api-token-id* (reaction (-> @routing/state* :route-params :api-token-id)))
(defonce data* (reagent/atom nil))

(defn fetch []
  (user-core/clean-and-fetch)
  (go (reset!
       data*
       (some->
        {:url (path :api-token {:user-id @user-core/user-id* :api-token-id @api-token-id*})
         :chan (async/chan)}
        http-client/request :chan <!
        http-client/filter-success! :body))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn edit-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:className "ml-3"
         :onClick #(do (core/clean-and-fetch)
                       (reset! show true))}
        "Edit"]
       [edit/dialog {:show @show
                     :on-hide #(reset! show false)
                     :on-save-callback clean-and-fetch}]])))

(defn delete-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:variant "danger"
         :className "ml-3"
         :onClick #(reset! show true)}
        "Delete"]
       [delete/dialog {:show @show
                       :on-hide #(reset! show false)}]])))

(defn page []
  [:article.user-api-token.my-5
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]

   (if-let [api-token @data*]
     [:div
      [:header.mb-5
       [breadcrumbs/main {:href (path :user {:user-id (-> api-token :user_id)})}]
       [:h1.mt-3 "API Token " [:code (-> api-token :token_part)]]
       [:h6 (:firstname @user-core/user-data*) " " (:lastname @user-core/user-data*)]]

      [:> Table {:striped true :borderless true}
       [:thead
        [:tr
         [:th "Property"]
         [:th "Value"]]]
       [:tbody
        [:tr
         [:td "Description"]
         [:td.name (-> api-token :description)]]
        [:tr
         [:td "Token Part"]
         [:td.name [:code (-> api-token :token_part) "..."]]]
        [:tr
         [:td "Permissions"]
         [:td.permission-summary (core/summarize-scopes api-token)]]
        [:tr
         [:td "Expires at"]
         [:td.name [humanize-datetime-component (-> api-token :expires_at)]]]
        [:tr
         [:td "Created at"]
         [:td.name [humanize-datetime-component (-> api-token :created_at)]]]
        (when-let [updated-at (-> api-token :updated_at)]
          [:tr
           [:td "Updated at"]
           [:td.name [humanize-datetime-component updated-at]]])
        [:tr
         [:td "ID"]
         [:td.name (-> api-token :id)]]]]
      [:div.mt-3
       [edit-button]
       [delete-button]]]

     [wait-component])])
