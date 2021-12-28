(ns leihs.admin.common.roles.components
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.common.roles.core :as roles]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.state :as state]

    [cljs-uuid-utils.core :as uuid]
    [taoensso.timbre :as logging]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))


(defn fetch-roles< [path]
  (let [chan (async/chan)]
    (go (>! chan (some-> {:chan (async/chan)
                          :url path}
                         http-client/request
                         :chan <! http-client/filter-success! :body)))
    chan))

(defn put-roles< [path roles]
  (let [chan (async/chan)]
    (go (>! chan (some-> {:chan (async/chan)
                          :method :put
                          :json-params roles
                          :url path}
                         http-client/request
                         :chan <! http-client/filter-success! :body)))
    chan))

(defn inner-roles-component
  [roles* edit-mode? & {:keys [compact on-change-handler]
                        :or {compact false
                             on-change-handler #()}}]
  (reagent/with-let [role-id-prefix (uuid/uuid-string (uuid/make-random-uuid))]
    [:div.mb-1
     (doall (for [role roles/hierarchy]
              (let [enabled (get @roles* role false)]
                (if (and compact (not enabled) (not edit-mode?))
                  ^{:key role} [:div]
                  ^{:key role} [:div.form-check
                                [:input.form-check-input
                                 {:id (str role-id-prefix "_" role)
                                  :type :checkbox
                                  :checked enabled
                                  :on-change #(reset! roles* (roles/set-roles role (not enabled)))
                                  :disabled (not edit-mode?)
                                  }]
                                [:label.form-check-label
                                 {:for (str role-id-prefix "_" role)}
                                 [:span " " role]]]))))]))

(defn edit-roles-form-elements [roles*]
  [inner-roles-component roles* true])

(defn edit-roles-header-component []
  [:h3 "Edit roles"])

(defn roles-edit-component [roles edit-mode?* & {:keys [update-handler]}]
  [:div
   [form-components/edit-modal-component
    roles edit-roles-header-component edit-roles-form-elements
    :abort-handler #(reset! edit-mode?* false)
    :submit-handler update-handler]])

(defn roles-component [roles & {:keys [compact update-handler]
                                :or {compact false
                                     update-handler nil}}]
  (reagent/with-let [edit-mode?* (reagent/atom false)]
    [:div
     (when @edit-mode?*
       [roles-edit-component roles edit-mode?*
        :update-handler (fn [updated-roles]
                          (reset! edit-mode?* false)
                          (update-handler updated-roles))])
     [:div [inner-roles-component (reagent/atom roles) false :compact compact]]
     (when update-handler
       [:div [:button.btn.btn-outline-primary
              {:class (when compact "btn-sm py-0")
               :on-click #(reset! edit-mode?* true)}
              [:span [icons/edit] " Edit"]]])
     (when @state/debug?*
       [:div.alert.alert-secondary.my-2
        [:h4 "Debug " 'roles-component]
        [:div [:h3 "@edit-mode?*"]
         [:pre (with-out-str (pprint @edit-mode?*))]]
        [:div [:h3 "roles"]
         [:pre (with-out-str (pprint roles))]]])]))

