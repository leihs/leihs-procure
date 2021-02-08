(ns leihs.admin.resources.inventory-pools.inventory-pool.suspension.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
    [leihs.admin.utils.regex :as regex]

    ["date-fns" :as date-fns]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]
    ["@leihs/ui-components" :as UI]
    ))

(defn suspended? [suspended-until ref-date]
  (if-not suspended-until
    false
    (if (date-fns/isBefore suspended-until ref-date)
      false
      true)))

(defn humanized-suspended-until-component [suspended-until]
  [:span
   (if-not (suspended? suspended-until (:timestamp @state/global-state*))
     [:span.text-success "Not suspended."]
     [:span.text-danger
      (if (date-fns/isAfter suspended-until (js/Date. "2098-01-01"))
        "Suspended forever."
        [:span "Suspended for "
         [humanize-datetime-component suspended-until :add-suffix false] "."])])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-suspension< [path]
  (let [chan (async/chan)]
    (go (>! chan  (some-> {:chan (async/chan)
                           :url path}
                          http-client/request
                          :chan <! http-client/filter-success! :body)))
    chan))

(defn put-suspension< [path data]
  (let [chan (async/chan)]
    (go (>! chan (some-> {:chan (async/chan)
                          :method :put
                          :json-params data
                          :url path}
                         http-client/request
                         :chan <! http-client/filter-success! :body)))
    chan))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn supension-inner-form-component [disabled data*]
  [:div.my-3
   [form-components/input-component data* [:suspended_until]
    :element (reagent/adapt-react-class UI/DatePicker)
    :disabled disabled
    :extra-props { :minDate "tomorrow" }
    :placeholder (when disabled "")
    :label "Suspended until"]

   [form-components/input-component data* [:suspended_reason]
    :element :textarea
    :rows 3
    :disabled disabled
    :label "Reason"]])

(defn suspension-submit-component [supension edit-data* edit-mode?*]
  (let [changed* (reaction (not= supension @edit-data*))]
    [:div.row.mt-1
     [:div.col
      (if @changed*
        [:button.btn.btn-outline-warning
         {:type :button
          :on-click #(reset! edit-mode?* false)}
         icons/delete " Cancel" ]
        [:button.btn.btn-outline-secondary
         {:type :button
          :on-click #(reset! edit-mode?* false)}
         icons/delete " Close" ])]
     [:div.col
      [form-components/save-submit-component
       :disabled (not @changed*)]]]))

(defn suspension-edit-header []
  [:h4 "Edit Suspension"])

(defn suspension-edit-form-elements [edit-data*]
  [:div
   [:div.row
    [:div.col
     [:div.float-right
      [:button.btn.btn-outline-warning
       {:type :button
        :on-click #(reset! edit-data* {})}
       [:span icons/delete " Reset suspension"]]]]]
   [supension-inner-form-component false edit-data*]])

(defn suspension-edit-component [suspension edit-mode?* update-handler]
  [:div
   [form-components/edit-modal-component
    suspension
    suspension-edit-header
    suspension-edit-form-elements
    :abort-handler #(reset! edit-mode?* false)
    :submit-handler #(do (reset! edit-mode?* false)
                         (update-handler %))]])

(defn suspension-component
  [data & {:keys [compact update-handler]
           :or {compact false
                update-handler nil}}]
  (reagent/with-let [edit-mode?* (reagent/atom false)]
    [:div.suspension
     (if (nil? data)
       [wait-component]
       (let [suspended-until (some-> data :suspended_until presence js/Date.)]
         [:div
          (when @edit-mode?*
            [suspension-edit-component data edit-mode?* update-handler])
          [:div
           [:div [humanized-suspended-until-component
                  suspended-until]]
           (when-not compact [supension-inner-form-component
                              true (reagent/atom data)])]
          [:div
           [:div
            (when (suspended? suspended-until (:timestamp @state/global-state*))
              [:button.btn.btn-warning.mr-2
               {:class (when compact "btn-sm py-0")
                :on-click #(update-handler {})}
               [:span icons/delete " Reset "]])
            [:button.btn.btn-outline-primary
             {:class (when compact "btn-sm py-0")
              :on-click #(reset! edit-mode?* true)}
             [:span icons/edit " Edit"]]]]]))]))
