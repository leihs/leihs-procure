(ns leihs.admin.resources.system.authentication-system.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.system.authentication-system.front :as authentication-system :refer [authentication-system-id*]]
    [leihs.admin.resources.system.authentication-system.users.shared :refer [authentication-system-users-filter-value]]
    [leihs.admin.resources.system.authentication-systems.breadcrumbs :as ass-breadcrumbs]
    [leihs.admin.resources.user.front.shared :as user-shared :refer [user-id*]]
    [leihs.admin.resources.users.front :as users]
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


(def authentication-system-users-count*
  (reaction (-> @users/data*
                (get (:url @routing/state*) {})
                :authentication-system_users_count)))

;### actions ##################################################################

;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-user [id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :authentication-system-user
                         {:authentication-system-id @authentication-system-id*
                          :user-id id})
              :method :put
              :json-params {}
              :query-params {}}
             {:modal true
              :title "Add user"
              :handler-key :authentication-system-users}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn add-clean-and-fetch [& args]
  (user-shared/fetch-user))


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce edit-data* (reagent/atom {}))

(defn fetch-authentication-system-user-data []
  (defonce fetch-authentication-system-user-data-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :authentication-system-user
                                          (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Authentication-System-User Data"
                               :handler-key :authentication-system-user-data
                               :retry-fn #'fetch-authentication-system-user-data}
                              :chan resp-chan)]
    (reset! fetch-authentication-system-user-data-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-authentication-system-user-data-id*))
            (reset! edit-data* (:body resp)))))))

(defn edit-user []
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :authentication-system-user
                         {:authentication-system-id @authentication-system-id*
                          :user-id @user-id*})
              :method :put
              :json-params @edit-data*
              :query-params {}}
             {:modal true
              :title "Edit user"
              :handler-key :authentication-system-users
              :retry-fn edit-user}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (routing/navigate!
              (path :authentication-system-users
                    {:authentication-system-id @authentication-system-id*})))))))

(defn edit-clean-and-fetch [& args]
  (reset! edit-data* {})
  (user-shared/fetch-user)
  (fetch-authentication-system-user-data))

(defn edit-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (edit-user))}
   [:div.form-group.data
    [:label "Data"]
    [:textarea.form-control
     {:auto-complete :false
      :value (-> @edit-data* :data presence)
      :on-change #(swap! edit-data* assoc :data (-> % .-target .-value presence))}]]
   [:div.form-group
    [:div.float-right
     [:button.btn.btn-primary
      {:type :submit}
      " Save " ]]
    [:div.clearfix]]])

(defn edit-page []
  [:div.authentication-system-users-edit
   [routing/hidden-state-component
    {:will-mount edit-clean-and-fetch
     :did-change edit-clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (ass-breadcrumbs/authentication-systems-li)
      (ass-breadcrumbs/authentication-system-li)
      (ass-breadcrumbs/authentication-system-users-li)
      (ass-breadcrumbs/authentication-system-user-edit-li)]
     [])
   [:h1 "Edit Authentication-System Data for User " [user-shared/user-name-component]]
   (if-not (contains? @edit-data* :data)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [edit-form-component])])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-user [user-id]
  (let [resp-chan (async/chan)
        id (requests/send-off
             {:url (path :authentication-system-user {:authentication-system-id @authentication-system-id* :user-id user-id})
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove user"
              :handler-key :authentication-system-users
              :retry-fn #(remove-user user-id)}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (users/fetch-users))))))

(defn action-th-component []
  [:th "Action"])

(defn action-td-component [user]
  [:td
   (if (:authentication_system_id user)
     [:span
      [:a.btn.btn-sm.btn-warning.mx-2
       {:key :edit
        :href (path :authentication-system-user-edit
                    {:authentication-system-id @authentication-system-id*
                     :user-id (:id user)})}
       icons/edit " Edit "]
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

(defn authentication-system-users-filter-on-change [& args]
  (accountant/navigate!
    (users/page-path-for-query-params
      {:page 1
       :authentication-system-users-only (not (authentication-system-users-filter-value
                                     (:query-params @routing/state*)))})))

(defn authentication-system-users-filter []
  [:div.form-authentication-system.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Authentication-System users only"]
    [:input
     {:type :checkbox
      :on-change authentication-system-users-filter-on-change
      :checked (authentication-system-users-filter-value (:query-params @routing/state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [authentication-system-users-filter]
    [users/form-term-filter]
    [users/form-admins-filter]
    [users/form-type-filter]
    [users/form-per-page]
    [users/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@authentication-system-users-count*"
      [:pre (with-out-str (pprint @authentication-system-users-count*))]]]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:will-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [filter-component]
   [:p "To add users disable the \"Authentication-System users only\" filter."]
   [users/pagination-component]
   [users/users-table-component colconfig]
   [users/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.authentication-system-users
   [routing/hidden-state-component
    {:will-mount (fn [_] (authentication-system/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (ass-breadcrumbs/authentication-systems-li)
      (ass-breadcrumbs/authentication-system-li)
      (ass-breadcrumbs/authentication-system-users-li)]
     [])
   [:div
    [:h1
     (let [c (or @authentication-system-users-count* 0)]
       [:span c " " (pluralize-noun c "User")
        [:span " in Authentication-System "]
        [authentication-system/name-component]])]
    [main-page-component]
    ]])
