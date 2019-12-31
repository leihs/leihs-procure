(ns leihs.admin.resources.system.system-admins.direct-users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.system-admins.direct-users.shared :refer [filter-value]]
    [leihs.admin.resources.system.system-admins.breadcrumbs :as sa-breadcrumbs]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(def system-admin-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :system-admin_users_count)))


;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-user [id]
  (let [resp-chan (async/chan)
        url (path :system-admins-direct-user
                  {:user-id id} {})
        id (requests/send-off
             {:url url
              :method :put
              :json-params {}
              :query-params {}}
             {:modal true
              :title "Add user"}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [user-id]
  (let [resp-chan (async/chan)
        url (path :system-admins-direct-user
                  {:user-id user-id})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove user"}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn action-th-component []
  [:th "Action"])

(defn action-td-component [user]
  [:td
   (if (:system_admin_user_id user)
     [:span
      [:button.btn.btn-sm.btn-danger.mx-2
       {:key :delete
        :on-click (fn [_] (remove-user (:id user)))}
       icons/delete " Remove "]]
     [:span
      [:button.btn.btn-sm.btn-primary.mx-2
       {:on-click #(add-user (:id user))}
       icons/add " Add "]])])

(def colconfig
  (merge users/default-colconfig
         {:email false
          :customcols [{:key :action
                        :th action-th-component
                        :td action-td-component}]}))


;### filter ###################################################################

(defn filter-on-change [& args]
  (accountant/navigate!
    (users/page-path-for-query-params
      {:page 1
       :system-admin-direct-users
       (not (filter-value
              (:query-params @routing/state*)))})))

(defn system-admin-users-filter []
  [:div.form-system-admin.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Direct system-admins only"]
    [:input
     {:type :checkbox
      :on-change filter-on-change
      :checked (filter-value (:query-params @routing/state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [system-admin-users-filter]
    [users/form-term-filter]
    [users/form-admins-filter]
    [users/form-type-filter]
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@system-admin-users-count*"
      [:pre (with-out-str (pprint @system-admin-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change users/escalate-query-paramas-update}]
   [filter-component]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn page []
  [:div.system-admin-users
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (sa-breadcrumbs/system-admins-li)
      (sa-breadcrumbs/system-admin-direct-users-li)] [])
   [:div
    [:h1
     (let [c (or @system-admin-users-count* 0)]
       [:span c " Direct " (pluralize-noun c "User")
        [:span " in System-Admins "]])]
    [main-page-component]]])
