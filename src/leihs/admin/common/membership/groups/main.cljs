(ns leihs.admin.common.membership.groups.main
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [go <!]]
   [leihs.admin.common.components.filter :as filter]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.common.membership.groups.shared :refer [default-query-params]]
   [leihs.admin.resources.groups.main :as groups]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]))

(defn form-membership-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.mr-1 {:for :groups-membership} " Membership "]
   [:select#groups-membership.custom-select
    {:value (:membership (merge default-query-params
                                (:query-params @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (groups/page-path-for-query-params
                                           {:page 1
                                            :membership val}))))}
    (doall (for [[k n] {"any" "members and non-members"
                        "non" "non-members"
                        "member" "members"}]
             [:option {:key k :value k} n]))]])

(defn filter-component []
  [filter/container
   [:<>
    [filter/form-term-filter-component]
    [filter/form-including-user]
    [form-membership-filter]
    [filter/form-per-page]
    [filter/reset]]])

(defn member-th-component [] [:th {:key :member} " Member "])

(defn remove-memebership [path group]
  (swap! groups/data* update-in
         [(:route @routing/state*) :groups (:page-index group) :member]
         #(identity nil))
  (go (when (some->
             {:chan (async/chan)
              :url path
              :method :delete}
             http-client/request
             :chan <! http-client/filter-success!)
        (groups/fetch-groups))))

(defn remove-memebership-component [path group]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (remove-memebership path group))}
   [:button.btn.btn-sm.btn-warning
    {:type :submit}
    [:span
     [icons/delete]
     " Remove "]]])

(defn add-membership [path group]
  (let [data-path [(:route @routing/state*) :groups (:page-index group) :member]]
    (swap! groups/data* update-in data-path #(identity nil))
    (go (when (some->
               {:chan (async/chan)
                :url path
                :method :put}
               http-client/request
               :chan <! http-client/filter-success!)
          (groups/fetch-groups)
          (swap! groups/data* update-in data-path #(identity false))))))

(defn add-membership-component [path group]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (add-membership path group))}
   [:button.btn.btn-sm.btn-primary
    {:type :submit}
    [:span [icons/add] " Add "]]])

(defn member-td-component [path-fn group]
  (let [path (path-fn group)]
    [:td.member {:key :member}
     [:div.form-row.align-items-center
      [:input.group_member
       {:id :group_member
        :type :checkbox
        :checked (:member group)
        :disabled true
        :readOnly true}]
      [:div.ml-2
       (case (:member group)
         true [remove-memebership-component path group]
         false [add-membership-component path group]
         nil [:button.btn.btn-sm.btn-secondary
              {:disabled true}
              [:span [icons/waiting]]])]]]))
