(ns leihs.admin.common.membership.users.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.membership.users.shared :refer [default-query-params]]
    [leihs.admin.resources.users.main :as users]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs.core.async :as async]
    [accountant.core :as accountant]
    [reagent.core :as reagent]))


;;; filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [users/form-term-filter]
     [users/form-enabled-filter]
     [routing/select-component
      :label "Membership"
      :query-params-key :membership
      :options {"any" "members and non-members"
                "non" "non-members"
                "member" "members"
                "direct" "direct members"
                "group" "group members"}
      :default-option "member"]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])


;;; member td ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member-user-th-component []
  [:th "Member"])

(defn member-user-td-component [user]
  [:td
   [:input
    {:id :member
     :type :checkbox
     :checked (:member user)
     :disabled true
     :readOnly true }]])


;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-aggregated-membership [user]
  (swap! users/data* update-in [(:route @routing/state*)
                                :users (:page-index user)]
         (fn [row]
           (assoc row :member (or (:direct_member row)
                                  (:group_member row))))))

(defn update-membership-in-table [new-state user]
  (swap! users/data* assoc-in [(:route @routing/state*)
                               :users (:page-index user)
                               :direct_member] new-state)
  (update-aggregated-membership user))

(defn direct-member-user-th-component []
  [:th "Direct member"])

(defn change-direct-membership
  [path user method]
  (let [new-state (case method
                    :put true
                    :delete false)]
    (go (when (some->
                {:chan (async/chan)
                 :url path
                 :method method}
                http-client/request :chan <!
                http-client/filter-success!)
          (update-membership-in-table new-state user)))))

(defn remove-direct-memebership [path user]
  (change-direct-membership path user :delete))

(defn remove-direct-memebership-component [path user]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (remove-direct-memebership path user))}
   [:button.btn.btn-warning.btn-sm
    {:type :submit}
    [:span
     [icons/delete]
     " Remove "]]])

(defn add-direct-memebership [path user]
  (change-direct-membership path user :put))

(defn add-direct-memebership-component [path user]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (add-direct-memebership path user))}
   [:button.btn.btn-sm.btn-primary
    {:type :submit}
    [:span [icons/add] " Add "]]])

(defn create-direct-member-user-td-component [path-fn]
  (fn [user]
    (let [path (path-fn user)]
      [:td.direct-member
       [:div
        [:div.form-row.align-items-center
         [:input
          {:id :direct_member
           :type :checkbox
           :checked (:direct_member user)
           :disabled true
           :readOnly true}]
         [:div.ml-2
          (case (:direct_member user)
            true [remove-direct-memebership-component path user]
            false [add-direct-memebership-component path user]
            nil [:button.btn.btn-secondary.btn-sm
                 {:disabled true}
                 [:span [icons/waiting]]])]]]])))


;;; group member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-member-user-th-component []
  [:th "Group member"])

(defn create-group-member-user-td-component [path-fn]
  (fn [user]
    [:td.group-member
     [:input
      {:id :group_member
       :type :checkbox
       :checked (:group_member user)
       :disabled true
       :readOnly true }]
     [:span.ml-2
      (if (:group_member user)
        [:a.btn.btn-outline-primary.btn-sm
         {:href (path-fn user {} {:membership "any"})}
         [:span [icons/edit] " Edit " ]]
        [:a.btn.btn-outline-primary.btn-sm
         {:href (path-fn user {} {:membership "any"})}
         [:span [icons/add] " Add "]])]]))


