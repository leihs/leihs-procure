(ns leihs.admin.resources.users.choose-main
  (:refer-clojure :exclude [keyword])
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.users.main :as users-main]
   [leihs.core.core :refer [keyword presence]]
   [leihs.core.routing.front :as routing]))

(defn choose-user-th-component []
  [:th {:key :choose} "Choose"])

(defn choose-user-td-component [user]
  [:td {:key :choose}
   [:a.btn.btn-sm.btn-primary
    {:href (if-let [href (-> @routing/state* :query-params-raw
                             :return-to presence)]
             (let [href-params (routing/dissect-href href)
                   uid (or (:email user) (:login user) (:id user))
                   query-params-key (-> @routing/state* :query-params-raw
                                        :query-params-key presence (or :user-uid)
                                        keyword)]
               (path (:handler-key href-params)
                     (:route-params href-params)
                     (assoc (:query-params-raw href-params) query-params-key uid)
                     (:fragment href-params)))
             "")}
    [:i.fas.fa-rotate-90.fa-hand-pointer]
    " Choose user "]])

(defn user-td-component [user]
  [:td [users-main/user-td-inner-component user]])

(defn table []
  [users-main/users-table
   [users-main/user-th-component
    choose-user-th-component]
   [user-td-component
    choose-user-td-component]])

(defn page []
  [:div
   [:h1.my-5
    [icons/users] " Choose user"]
   [users-main/filter-component]
   [table/toolbar]
   [table]
   [table/toolbar]])
