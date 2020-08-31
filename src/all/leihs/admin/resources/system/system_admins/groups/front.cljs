(ns leihs.admin.resources.system.system-admins.groups.front
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
    [leihs.admin.resources.system.system-admins.groups.shared :refer [filter-value]]
    [leihs.admin.resources.system.system-admins.breadcrumbs :as sa-breadcrumbs]
    [leihs.admin.resources.groups.front :as groups]
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


(def system-admin-groups-count*
  (reaction (-> @groups/data*
                (get (:url @routing/state*) {})
                :system-admin_groups_count)))


;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-group [id]
  (let [resp-chan (async/chan)
        url (path :system-admins-group
                  {:group-id id} {})
        id (requests/send-off
             {:url url
              :method :put
              :json-params {}
              :query-params {}}
             {:modal true
              :title "Add group"}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (groups/fetch-groups))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn remove-group [group-id]
  (let [resp-chan (async/chan)
        url (path :system-admins-group
                  {:group-id group-id})
        id (requests/send-off
             {:url url
              :method :delete
              :query-params {}}
             {:modal true
              :title "Remove group"}
             :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 204))
            (groups/fetch-groups))))))

(defn action-th-component []
  [:th "Action"])

(defn action-td-component [group]
  [:td
   (if (:system_admin_group_id group)
     [:span
      [:button.btn.btn-sm.btn-danger.mx-2
       {:key :delete
        :on-click (fn [_] (remove-group (:id group)))}
       icons/delete " Remove "]]
     [:span
      [:button.btn.btn-sm.btn-primary.mx-2
       {:on-click #(add-group (:id group))}
       icons/add " Add "]])])


;### filter ###################################################################

(defn filter-on-change [& args]
  (accountant/navigate!
    (groups/page-path-for-query-params
      {:page 1
       :system-admin-groups
       (not (filter-value
              (:query-params @routing/state*)))})))

(defn system-admin-groups-filter []
  [:div.form-system-admin.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "System-Admin groups only"]
    [:input
     {:type :checkbox
      :on-change filter-on-change
      :checked (filter-value (:query-params @routing/state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [system-admin-groups-filter]
    [groups/form-term-filter]
    [groups/form-org-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@system-admin-groups-count*"
      [:pre (with-out-str (pprint @system-admin-groups-count*))]]]))

(defn td-actions-component [group]
  [:td
   {:key :actions}
   (if (:system_admin_group_id group)
     [:span
      [:button.btn.btn-sm.btn-danger.mx-2
       {:key :delete
        :on-click (fn [_] (remove-group (:id group)))}
       icons/delete " Remove "]]
     [:span
      [:button.btn.btn-sm.btn-primary.mx-2
       {:on-click #(add-group (:id group))}
       icons/add " Add "]])])

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-change groups/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [groups/groups-table-component
    [[:th {:key :actions} "Actions"]]
    [td-actions-component]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:div.system-admin-groups
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (sa-breadcrumbs/system-admins-li)
      (sa-breadcrumbs/system-admin-groups-li)][])
   [:div
    [:h1
     (let [c (or @system-admin-groups-count* 0)]
       [:span c " " (pluralize-noun c "System-Admins-Group")])]
    [main-page-component]]])
