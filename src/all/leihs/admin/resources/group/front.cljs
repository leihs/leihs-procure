(ns leihs.admin.resources.group.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.icons :as icons]
    [leihs.admin.front.requests.core :as requests]
    [leihs.admin.front.shared :refer [humanize-datetime-component short-id gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.utils.core :refer [keyword str presence]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(declare fetcher-group)
(defonce group-id* (reaction (-> @state/routing-state* :route-params :group-id)))
(defonce group-data* (reagent/atom nil))


(defonce edit-mode?*
  (reaction
    (and (map? @group-data*)
         (boolean ((set '(:group-edit :group-add))
                   (:handler-key @state/routing-state*))))))

(def fetch-group-id* (reagent/atom nil))
(defn fetch-group []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :group (-> @state/routing-state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Group"
                               :handler-key :group
                               :retry-fn #'fetch-group}
                              :chan resp-chan)]
    (reset! fetch-group-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-group-id*))
            (reset! group-data* (:body resp)))))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-and-fetch [& args]
  (reset! group-data* nil)
  (fetch-group))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn field-component
  ([kw]
   (field-component kw {}))
  ([kw opts]
   (let [opts (merge {:type :text :node-type :input} opts)]
     [:div.form-group.row
      [:label.col.col-form-label.col-sm-2 {:for kw} kw]
      [:div.col.col-sm-10
       [:div.input-group
        (if @edit-mode?*
          [(:node-type opts) 
           {:class :form-control
            :id kw
            :type (:type opts)
            :value (or (kw @group-data*) "")
            :on-change #(swap! group-data* assoc kw (-> % .-target .-value presence))
            :disabled (not @edit-mode?*)}]
          [:div
           (if-let [value (-> @group-data* kw presence)]
             [:span.form-control-plaintext
              (case (:type opts)
                :email [:a {:href (str "mailto:" value)}
                        [:i.fas.fa-envelope] " " value]
                :url [:a {:href value} value]
                value)])])]]])))

(defn checkbox-component [kw]
  [:div.form-check.form-check-inline
   [:label {:for kw}
    [:input
     {:id kw
      :type :checkbox
      :checked (kw @group-data*)
      :on-change #(swap! group-data* assoc kw (-> @group-data* kw boolean not))
      :disabled (not @edit-mode?*)}]
    [:span.ml-2 kw]]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.group-debug
     [:hr]
     [:div.edit-mode?*
      [:h3 "@edit-mode?*"]
      [:pre (with-out-str (pprint @edit-mode?*))]]
     [:div.group-data
      [:h3 "@group-data*"]
      [:pre (with-out-str (pprint @group-data*))]]]))


;; group components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn basic-component []
  [:div.form.mt-2
   [:h3 "Basic properties"]
   [field-component :name]
   [field-component :description {:node-type :textarea}]
   [field-component :org_id]])

(defn additional-properties-component []
  (when-let [group-data @group-data*]
    [:div.additional-properties
     [:p 
      [:span "This group has been " 
       [:b " created " [humanize-datetime-component (:created_at group-data)]] ", and " 
       [:b " updated " [humanize-datetime-component (:updated_at group-data)]] ". "]
      (let [users-count (:users_count group-data)]
        [:span (if (= 0 users-count)
                 "This group has no users."
                 [:span 
                  "This group has " 
                  [:strong users-count " " (pluralize-noun users-count "user")] "."])])]]))

(defn group-component []
  [:div.group-component
   (if (nil?  @group-data*)
     [:div.text-center
      [:i.fas.fa-spinner.fa-spin.fa-5x]
      [:span.sr-only "Please wait"]]
     [:div 
      [:div [basic-component]]
      [:div [additional-properties-component]]])])

(defn group-name-component []
  (if-not @group-data*
    [:span {:style {:font-family "monospace"}} (short-id @group-id*)]
    [:em (str (:name @group-data*))]))

(defn group-id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @group-data*)]])

(defn org-warning-component []
  (when (:org_id @group-data*)
    [:div.alert.alert-warning
     [:p "The property " [:code "org_id"]
      " is conventionally reserved for groups maintained by an automatized import or sync via the API. "
      " If this instance of leihs is using such a mechanism it will likely " [:b "override any changes submitted here." ]]]))

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.group
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/groups-li)
      (breadcrumbs/group-li @group-id*)]
     [(breadcrumbs/group-users-li @group-id*)
      (breadcrumbs/group-delete-li @group-id*)
      (breadcrumbs/group-edit-li @group-id*)])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Group "]
      [group-name-component]]
     [group-id-component]]]
   [group-component]
   [debug-component]])


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :group {:group-id @group-id*})
                               :method :patch
                               :json-params  @group-data*}
                              {:modal true
                               :title "Update Group"
                               :handler-key :group-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :group {:group-id @group-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [org-warning-component]
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-group
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/groups-li)
      (breadcrumbs/group-li @group-id*)
      (breadcrumbs/group-edit-li @group-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Group "]
      [group-name-component]]
     [group-id-component]]]
   [group-component]
   [patch-submit-component]
   [debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :groups)
                               :method :post
                               :json-params  @group-data*}
                              {:modal true
                               :title "Add Group"
                               :handler-key :group-add
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :group {:group-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [org-warning-component]
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Add "]]
     [:div.clearfix]]))

(defn add-page []
  [:div.new-group
   [state/hidden-routing-state-component
    {:will-mount #(reset! group-data* {})}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/groups-li)
      (breadcrumbs/group-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Add Group "]]]]
   [group-component]
   [create-submit-component]
   [debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transfer-data* (reagent/atom {}))

(defn delete-group [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :group (-> @state/routing-state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete Group"
                               :handler-key :group-delete
                               :retry-fn #'delete-group}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :groups {}
                    (-> @state/global-state* :groups-query-params))))))))

(defn delete-submit-component []
  [:div.form
   [org-warning-component]
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click delete-group}
     icons/delete
     " Delete "]]
   [:div.clearfix]])

(defn delete-component []
  [:div
   [:h2 "Delete Group"]
   [:div.float-right
    [:button.btn.btn-warning.btn-lg
     {:on-click delete-group}
     [:i.fas.fa-times] " Delete"]]])

(defn delete-page []
  [:div.group-delete
   [state/hidden-routing-state-component
    {:will-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/groups-li]
      [breadcrumbs/group-li @group-id*]
      [breadcrumbs/group-delete-li @group-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete Group "
    [group-name-component]]
   [group-id-component]
   [delete-submit-component]])
