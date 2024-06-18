(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.shared
  (:require
   [clojure.set :refer [rename-keys]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Form]]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre]))

(defonce data* (reagent/atom {}))

(defn set-user-id-from-params [& _]
  (let [params (get @routing/state* :query-params)]
    (reset! data* (merge @delegation/delegation*
                         (rename-keys params {:user-uid :responsible_user_id})))
    (when (empty? @data*)
      (reset! data* {:pool_protected true})))
  data*)

(def user-delegation*
  (reaction (set-user-id-from-params)))

(defn responsible-user-choose-component []
  [:div.input-group-append
   [:a.btn.btn-primary
    {:tab-index form-components/TAB-INDEX
     :href (path :users-choose {}
                 {:return-to (path (:handler-key @routing/state*)
                                   (:route-params @routing/state*)
                                   @data*)})}
    [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
    " Choose responsible user "]])

(defn delegation-form [& {:keys [id action]
                          :or {id "delegation-form"}}]
  (let [data* (set-user-id-from-params)]
    (fn []
      [:> Form {:id id
                :on-submit (fn [e]
                             (.preventDefault e)
                             (action @data*))}
       [form-components/input-component data* [:name]
        :label "Name"]
       [form-components/input-component data* [:responsible_user_id]
        :label "Responsible user"
        :append responsible-user-choose-component]
       [:div
        [form-components/checkbox-component data* [:pool_protected]
         :label "Protected"
         :hint [:span
                "An " [:strong " unprotected "]
                " delegation can be " [:strong "added"] " to any other pool and then be used and "
                [:strong  " modified "] " from those pools in every way."
                " You can unprotect a delegation temporarily to share it with a limited number of pools."]]]])))
