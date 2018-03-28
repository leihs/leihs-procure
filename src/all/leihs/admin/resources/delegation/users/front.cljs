(ns leihs.admin.resources.delegation.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.resources.delegation.users.shared :refer [delegation-users-filter-value]]
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.front.icons :as icons]
    [leihs.admin.resources.delegation.front :as delegation :refer [delegation-id*]]
    [leihs.admin.resources.users.front :as users]

    [leihs.admin.utils.core :refer [keyword str presence]]
    [leihs.admin.utils.regex :as regex]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(def delegation-users-count* 
  (reaction (-> @users/data*
                (get (:url @state/routing-state*) {})
                :delegation_users_count)))

;### actions ##################################################################

(defn add-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off 
             {:url (path :delegation-user {:delegation-id @delegation-id* :user-id user-id})
              :method :put
              :query-params {}}
             {:modal true
              :title "Add user"
              :handler-key :delegation-users
              :retry-fn #(add-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn remove-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off 
             {:url (path :delegation-user {:delegation-id @delegation-id* :user-id user-id})
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove user"
              :handler-key :delegation-users
              :retry-fn #(remove-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn action-th-component []
  [:th "Add or remove from this delegation"])

(defn action-td-component [user]
  [:td 
   (if (:delegation_id user)
     [:button.btn.btn-sm.btn-danger
      {:on-click (fn [_] (remove-user (:id user)))}
      icons/delete " Remove "]
     [:button.btn.btn-sm.btn-primary
      {:on-click (fn [_] (add-user (:id user)))}
      icons/add " Add "])])

(def colconfig  
  (merge users/default-colconfig
         {:email false
          :customcols [{:key :action
                        :th action-th-component
                        :td action-td-component}]}))


;### filter ###################################################################

(defn delegation-users-filter-on-change [& args]
  (accountant/navigate! 
    (users/page-path-for-query-params 
      {:page 1
       :delegation-users-only (not (delegation-users-filter-value 
                                     (:query-params @state/routing-state*)))})))

(defn delegation-users-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Delegation users only"]
    [:input
     {:type :checkbox
      :on-change delegation-users-filter-on-change
      :checked (delegation-users-filter-value (:query-params @state/routing-state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [delegation-users-filter]
    [users/form-term-filter]
    [users/form-admins-filter]
    [users/form-role-filter]
    [users/form-type-filter]
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@delegation-users-count*"
      [:pre (with-out-str (pprint @delegation-users-count*))]]]))

(defn main-page-component []
  [:div
   [state/hidden-routing-state-component
    {:will-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [:p "To add users disable the \"Delegation users only\" filter."]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.delegation-users
   [state/hidden-routing-state-component
    {:will-mount (fn [_] (delegation/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/delegations-li)
      (breadcrumbs/delegation-li @delegation/delegation-id*)
      (breadcrumbs/delegation-users-li @delegation/delegation-id*)]
     [])
   [:div
    [:h1
     (let [c (or @delegation-users-count* 0)]
       [:span c " " (pluralize-noun c "User")
        [:span " in Delegation "]
        [delegation/delegation-name-component]])]
    [main-page-component]
    ]])
