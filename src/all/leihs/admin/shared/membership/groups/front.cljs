(ns leihs.admin.shared.membership.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.shared.membership.groups.shared :refer [default-query-params]]

    [cljs.core.async :as async]
    [accountant.core :as accountant]
    [reagent.core :as reagent]))


(defn form-membership-filter []
  [:div.form-group.ml-2.mr-2.mt-2
   [:label.mr-1 {:for :groups-membership} " Membership "]
   [:select#groups-membership.form-control
    {
     :value (:membership (merge default-query-params
                                (:query-params @routing/state*)))
     :on-change (fn [e]
                  (let [val (or (-> e .-target .-value presence) "")]
                    (accountant/navigate! (groups/page-path-for-query-params
                                            {:page 1
                                             :membership val}))))}
    (doall (for [[k n] {"any" "members and non-members"
                        "non" "non-members"
                        "member" "members"}]
             [:option {:key k :value k} n] ))]])


(def member-th-component [:th {:key :member} " Member "])


(defn remove-memebership [path group]
  (swap! groups/data* update-in
         [(:url @routing/state*) :groups (:page-index group) :member]
         #(identity nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url path
                               :method :delete}
                              {:modal true
                               :title "Remove Membership" }
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (< (:status resp) 300)
            (groups/fetch-groups))))))

(defn remove-memebership-component [path group]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (remove-memebership path group))}
   [:button.btn.btn-sm.btn-warning
    {:type :submit}
    [:span
     icons/delete
     " remove "]]])

(defn add-memebership [path group]
  (let [data-path [(:url @routing/state*) :groups (:page-index group) :member]]
    (swap! groups/data* update-in data-path #(identity nil))
    (let [resp-chan (async/chan)
          id (requests/send-off {:url path
                                 :method :put}
                                {:modal true
                                 :title "Add Membership"}
                                :chan resp-chan)]
      (go (let [resp (<! resp-chan)]
            (if (< (:status resp) 300)
              (groups/fetch-groups)
              (swap! groups/data* update-in data-path #(identity false))))))))

(defn add-memebership-component [path group]
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (add-memebership path group))}
   [:button.btn.btn-sm.btn-primary
    {:type :submit}
    [:span
     icons/add
     " add "]]])


(defn member-td-component [path-fn group]
  (let [path (path-fn group)]
    [:td.member {:key :member}
     [:div.form-row.align-items-center
      [:input
       {:id :_member
        :type :checkbox
        :checked (:member group)
        :disabled true
        :readOnly true }]
      [:div.ml-2
       (case (:member group)
         true [remove-memebership-component path group]
         false [add-memebership-component path group]
         nil [:button.btn.btn-sm.btn-secondary
              {:disabled true}
              [:span icons/waiting]])]]]))
