(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.main
  (:require [cljs.core.async :as async :refer [go <!]]
            [cljs.pprint :refer [pprint]]
            [leihs.admin.common.http-client.core :as http-client]
            [leihs.admin.common.icons :as icons]
            [leihs.admin.common.roles.components :refer [put-roles<
                                                         roles-component]]
            [leihs.admin.common.roles.core :as roles]
            [leihs.admin.paths :as paths :refer [path]]
            [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
            [leihs.admin.state :as state]
            [leihs.core.routing.front :as routing]
            [reagent.core :as reagent :refer [reaction]]))

(def roles-path*
  (reaction (path :inventory-pool-user-groups-roles
                  (-> @routing/state* :route-params))))

(def data* (reagent/atom nil))

(defn fetch [& _]
  (go (reset!
       data*
       (-> {:chan (async/chan)
            :url @roles-path*}
           http-client/request
           :chan <! http-client/filter-success! :body :groups-roles))))

(defn debug-component2 []
  (when @state/debug?*
    [:div.alert.alert-secondary
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn groups-roles-component2
  [update-notifier & {:keys [user-uid]}]
  [:div.groups-roles-component
   [routing/hidden-state-component
    {:did-mount #(do (reset! data* nil) (fetch))}]
   [debug-component2]
   (let [groups-roles (->> @data*
                           (map :roles)
                           (roles/aggregate))]
     [roles-component groups-roles])

   [:a.btn.btn-outline-primary.btn-sm.py-0
    {:href (path :inventory-pool-groups
                 {:inventory-pool-id @inventory-pool/id*}
                 {:including-user (or user-uid  (-> @routing/state* :route-params :user-id))
                  :role ""})}
    [icons/view] " " [icons/edit] " Roles via groups "]

   (doall
    (for [group-roles @data*]
      [:div {:key (:group_id group-roles)}
       [:h4.mb-0.mt-3
        "Roles "
        [:span
         " via the group "
         [:a {:href (path :group {:group-id (:group_id group-roles)})}
          [:em (:group_name group-roles)]]]]
       [roles-component (:roles group-roles)
        :compact true
        :update-handler
        #(go (swap! data* assoc-in [(:page-index group-roles) :roles]
                    (<! (put-roles<
                         (path :inventory-pool-group-roles
                               {:inventory-pool-id @inventory-pool/id*
                                :group-id (:group_id group-roles)}) %)))
             (update-notifier))]]))])
