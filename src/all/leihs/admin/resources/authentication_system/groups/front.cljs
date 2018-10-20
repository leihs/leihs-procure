(ns leihs.admin.resources.authentication-system.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.front.breadcrumbs :as admin-breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.utils.regex :as regex]

    [leihs.admin.resources.authentication-system.front :as authentication-system]
    [leihs.admin.resources.authentication-system.groups.shared :refer [filter-value]]
    [leihs.admin.resources.authentication-systems.breadcrumbs :as breadcrumbs]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))


(def authentication-system-groups-count*
  (reaction (-> @groups/data*
                (get (:url @routing/state*) {})
                :authentication-system_groups_count)))


;;; add ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-group [id]
  (let [resp-chan (async/chan)
        url (path :authentication-system-group 
                  {:authentication-system-id @authentication-system/authentication-system-id*
                   :group-id id}{})
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
				url (path :authentication-system-group 
									{:authentication-system-id @authentication-system/authentication-system-id*
									 :group-id group-id}{})
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


;### filter ###################################################################

(defn filter-on-change [& args]
  (accountant/navigate!
    (groups/page-path-for-query-params
      {:page 1
       :authentication-system-groups
       (not (filter-value
              (:query-params @routing/state*)))})))

(defn authentication-system-groups-filter []
  [:div.form-authentication-system.ml-2.mr-2.mt-2
   [:label
    [:span.pr-1 "Authentication-System groups only"]
    [:input
     {:type :checkbox
      :on-change filter-on-change
      :checked (filter-value (:query-params @routing/state*))
      }]]])

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-inline
    [authentication-system-groups-filter]
    [groups/form-term-filter]
    [groups/form-type-filter]
    [groups/form-per-page]
    [groups/form-reset]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div
     [:div "@authentication-system-groups-count*"
      [:pre (with-out-str (pprint @authentication-system-groups-count*))]]]))

(defn td-actions-component [group]
  [:td
   {:key :actions}
   (js/console.log (with-out-str (pprint group)))
   (if (:authentication_system_group_id group)
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
   [groups/pagination-component]
   [groups/groups-table-component
    [[:th {:key :actions} "Actions"]]
    [td-actions-component]]
   [groups/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn page []
	[:div.authentication-system-groups
	 (admin-breadcrumbs/nav-component
		 [(admin-breadcrumbs/leihs-li)
			(admin-breadcrumbs/admin-li)
			(breadcrumbs/authentication-systems-li)
			(breadcrumbs/authentication-system-li)
			(breadcrumbs/authentication-system-groups-li)
			][])
	 [:div
		[:h1
		 (let [c (or @authentication-system-groups-count* 0)]
			 [:span c " " (pluralize-noun c "Authentication-System-Group")
				[:span " in Authentication-System "]
				[authentication-system/name-component]])]
		[main-page-component]]])
