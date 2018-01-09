(ns leihs.admin.resources.api-tokens.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.resources.api-token.front :as api-token]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.state :as state :refer [routing-state*]]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.auth.core :as auth]
    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.front.shared :refer [humanize-datetime-component]]

    [cljs.pprint :refer [pprint]]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [reagent.core :as reagent]

    ))

(defonce api-tokens* (reagent/atom nil))

(defonce user-id* (reaction (-> @state/routing-state* :route-params :user-id)))

(def fetch-tokens-id* (reagent/atom nil))
(defn fetch-tokens [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :api-tokens {:user-id @user-id*})
                               :method :get}
                              {:modal false
                               :title "Fetch Api-Tokens"
                               :handler-key :api-tokens
                               :retry-fn #'fetch-tokens}
                              :chan resp-chan)]
    (reset! fetch-tokens-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200) ;success
                     (= id @fetch-tokens-id*) ;still the most recent request
                     (reset! api-tokens* (->> resp :body :api-tokens
                                              (map #(assoc % :key (:id %)))))))))))

(def scopes [:scope_read :scope_write :scope_admin_read :scope_admin_write])

(defn thead-component []
  [:thead
   [:tr
    [:th
     {:key :token_part}
     "token part"]
    (for [scope scopes]
      [:th
       {:key scope}
       [api-token/scope-text scope]]
      )
    [:th
     {:key :created}
     "created"]
    [:th
     {:key :expires_at}
     "expires"]]])

(defn api-token-tr-component [api-token]
  [:tr
   {:key (:id api-token)}
   [:td.token-part
    {:key :token-part}
    [:code
     [:a
      {:href (path :api-token {:user-id @user-id* :api-token-id (:id api-token)})}
      (:token_part api-token)]]]
   (for [scope scopes]
     [:td
      {:key scope}
      (with-out-str (pprint (scope api-token)))])
   [:td
    {:key :created_at}
    [humanize-datetime-component
     (:created_at api-token)]]
   [:td
    {:key :expires_at}
    [humanize-datetime-component
     (:expires_at api-token)]]])

(defn api-tokens-component []
  (if (= nil @api-tokens*)
    [:div.text-center
     [:i.fas.fa-spinner.fa-spin.fa-5x]]
    (if (empty? @api-tokens*)
      [:div
       [:p "There are currently no api-tokens for this user."]]
      [:table.table.table-striped.table-sm
       [thead-component]

       [:tbody
        (for [api-token @api-tokens*]
          [api-token-tr-component api-token])]])))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.debug
     [:h3 "@api-tokens*"]
     [:pre (with-out-str (pprint @api-tokens*))]]))

(defn page []
  [:div.account
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)
      (breadcrumbs/api-tokens-li @user-id*)]
     [(breadcrumbs/api-token-new-li @user-id*)])
   [:div
    [state/hidden-routing-state-component {:did-update fetch-tokens
                                           :did-mount fetch-tokens}]
    [:h1 " API-Tokens "]
    [api-tokens-component]
    [debug-component]
    ]])


