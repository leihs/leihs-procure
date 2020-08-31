(ns leihs.admin.shared.membership.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.shared.membership.users.shared :refer [default-query-params]]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs.core.async :as async]
    [accountant.core :as accountant]
    [reagent.core :as reagent]))


;;; filter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-membership-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.mr-1 {:for :users-membership} " Membership "]
   [:select#users-membership.form-control
    {:value (:membership (merge default-query-params
                                (:query-params @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (users/page-path-for-query-params
                                            {:page 1
                                             :membership val}))))}
    (doall (for [[k n] {"any" "members and non-members"
                        "non" "non-members"
                        "member" "members"
                        "direct" "direct-members"
                        "group" "group-members"}]
             [:option {:key k :value k} n] ))]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
    [:div.form-row
     [users/form-term-filter]
     [users/form-enabled-filter]
     [form-membership-filter]
     [routing/form-per-page-component]
     [routing/form-reset-component]]]])


;;; member td ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member-user-th-component []
  [:th "Member"])

(defn member-user-td-component [user]
  [:td
   [:input
    {:id :_member
     :type :checkbox
     :checked (:member user)
     :disabled true
     :readOnly true }]])

(def member-user-conf
  {:key :member
   :th member-user-th-component
   :td member-user-td-component})


;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-user-th-component []
  [:th "Direct member"])

(defn remove-direct-memebership [path user]
  (swap! users/data* update-in
         [(:url @routing/state*) :users  (:page-index user) :direct_member]
         #(identity nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url path
                               :method :delete}
                              {:modal true
                               :title "Remove Direct-Membership"}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (users/fetch-users)))))

(defn remove-direct-memebership-component [path user]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (remove-direct-memebership path user))}
   [:button.btn.btn-warning.btn-sm
    {:type :submit}
    [:span
     icons/delete
     " remove "]]])

(defn add-direct-memebership [path user]
  (swap! users/data* update-in
         [(:url @routing/state*) :users  (:page-index user) :direct_member]
         #(identity nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url path :method :put}
                              {:modal true
                               :title "Add Direct-Membership"}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)] (users/fetch-users)))))

(defn add-direct-memebership-component [path user]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (add-direct-memebership path user))}
   [:button.btn.btn-sm.btn-primary
    {:type :submit}
    [:span icons/add " add "]]])

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
               [:span icons/waiting]])]]]])))


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
         [:span icons/edit " edit " ]]
        [:a.btn.btn-outline-primary.btn-sm
         {:href (path-fn user {} {:membership "any"})}
         [:span icons/add " add "]])]]))


