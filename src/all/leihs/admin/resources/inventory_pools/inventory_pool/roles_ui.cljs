(ns leihs.admin.resources.inventory-pools.inventory-pool.roles-ui
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]

    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy allowed-roles-states]]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [cljs-uuid-utils.core :as uuid]
    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.string :as string]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))

(defn inline-roles-form-submit-component
  [roles changes* edit-mode?* id-prefix]
  (when @edit-mode?*
    [:div.row.mt-1
     [:div.col
      (if (not= roles @changes*)
        [:button.btn.btn-outline-warning
         {:type :button
          :on-click #(do (reset! changes* roles)
                         (reset! edit-mode?* false))}
         icons/delete " Cancel" ]
        [:button.btn.btn-outline-secondary
         {:type :button
          :on-click #(reset! edit-mode?* false)}
         icons/delete " Close" ])]
     [:div.col
      [form-components/save-submit-component
       :disabled (= roles @changes*)]]]))

(defn inline-roles-form-main-component
  [roles changes* edit-mode?* id-prefix]
  [:div
   (doall (for [role roles-hierarchy]
            (let [enabled (get (if @edit-mode?* @changes* roles) role false)
                  id (str id-prefix "-" role)]
              (if (and (not enabled) (not @edit-mode?*))
                ^{:key role} [:div]
                ^{:key role} [:div.form-check
                              [:input.form-check-input
                               {:id id
                                :type :checkbox
                                :checked enabled
                                :on-change #(reset! changes*
                                                    (roles/set-roles role (not enabled)))
                                :disabled (not @edit-mode?*)}]
                              [:label.form-check-label
                               {:for id}
                               [:span " " role]]]))))])

(defn inline-roles-form-component
  [roles on-submit-handler]
  (reagent/with-let [id-prefix (uuid/uuid-string (uuid/make-random-uuid))
                     changes* (reagent/atom roles)
                     edit-mode?* (reagent/atom false)]
    [:div
     (if-not @edit-mode?*
       [:div
        [inline-roles-form-main-component roles changes* edit-mode?* id-prefix]
        [:button.btn.btn-outline-primary.btn-sm.py-0.px-1
         {:on-click #(do (reset! edit-mode?* true)
                         (reset! changes* roles))}
         (if (:customer roles)
           [:span icons/edit " Edit"]
           [:span icons/add " Add"])]]
       [:div.text-left {:style {:opacity "1.0" :z-index 10000}}
        [:div.modal {:style {:display "block" :z-index 10000}}
         [:div.modal-dialog
          [:div.modal-content
           [:div.modal-header [:h4 "Roles"]]
           [:div.modal-body
            [:form.form
             {:on-submit (fn [e]
                           (let [success-chan (async/chan)]
                             (.preventDefault e)
                             (on-submit-handler @changes* success-chan)
                             (go (let [roles (<! success-chan)]
                                   (logging/warn 'roles roles)
                                   (reset! changes* roles)
                                   (reset! edit-mode?* false)))))}
             [inline-roles-form-main-component
              roles changes* edit-mode?* id-prefix]
             [inline-roles-form-submit-component roles
              changes* edit-mode?* id-prefix]]]]]]
        [:div.modal-backdrop {:style {:opacity "0.5"}}]])]))

