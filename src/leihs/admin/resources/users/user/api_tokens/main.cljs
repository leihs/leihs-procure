(ns leihs.admin.resources.users.user.api-tokens.main
  (:require [cljs.core.async :as async :refer [<! go]]
            [leihs.admin.common.components.table :as table]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.users.user.api-tokens.core :as core]
            [leihs.admin.resources.users.user.api-tokens.create :as create]
            [leihs.admin.resources.users.user.core :as user-core :refer [user-data*
                                                                         user-id*]]
            [leihs.admin.utils.misc :as front-shared :refer [humanize-datetime-component
                                                             wait-component]]
            [leihs.core.routing.front :as routing]
            [react-bootstrap :as react-bootstrap :refer [Alert Button]]
            [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch []
  (go (reset!
       data*
       (some->
        {:chan (async/chan)
         :url (path :user-api-tokens
                    (-> @routing/state* :route-params))}
        http-client/request :chan <!
        http-client/filter-success!
        :body :user-api-tokens))))

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch))

(defn add-button []
  (let [show (reagent/atom false)]
    (fn []
      [:<>
       [:> Button
        {:onClick #(do (core/reset-to-defaults)
                       (reset! show true))}
        "Add API Token"]
       [create/dialog {:show @show
                       :onHide #(reset! show false)}]])))

(defn thead-component []
  [:tr {:key "head"}
   [:th {:key :description} "Description"]
   [:th {:key :token_part} "Token Part"]
   [:th {:key :permissions} "Permission"]
   [:th {:key :created} "Created"]
   [:th {:key :expires_at} "Expires"]])

(defn tr-component [api-token]
  (let [api-token-path (path :user-api-token {:user-id @user-id* :api-token-id (:id api-token)})]
    [:tr
     [:td {:key :description}
      [:a {:href api-token-path} (or (:description api-token) "(no description)")]]
     [:td.token-part {:key :token-part}
      [:code (:token_part api-token) "..."]]
     [:td.permissions {:key :permissions}
      (core/summarize-scopes api-token)]
     [:td {:key :created_at}
      [humanize-datetime-component (:created_at api-token)]]
     [:td {:key :expires_at}
      [humanize-datetime-component (:expires_at api-token)]]]))

(defn table-component []
  [:div.user-api-tokens.mb-5
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]

   (if (and @data* @user-data*)
     [:<>
      (if-let [api-tokens (seq @data*)]
        [table/container
         {:className "user-api-tokens"
          :header [thead-component]
          :body (for [api-token api-tokens]
                  ^{:key (:id api-token)}
                  [tr-component api-token])}]
        [:> Alert {:variant "info"
                   :className "text-center mt-3"}
         "No API Tokens"])
      [add-button]]
     [wait-component])])
